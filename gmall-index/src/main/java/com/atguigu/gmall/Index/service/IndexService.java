package com.atguigu.gmall.Index.service;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.Index.aspect.GmallCache;
import com.atguigu.gmall.Index.feign.GmallPmsClient;
import com.atguigu.gmall.Index.utils.DistributedLock;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {
    // 远程调用
    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private DistributedLock distributedLock;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //设置key前缀
    private static final String KEY_PREFIX = "index:category:";

    @Autowired
    private RedissonClient redissonClient;
    /*
    redis 缓存
    RedisTemplate 可读性较差 只能通过程序读取,人工读取困难,所以很少直接使用RedisTemplate
    一定要使用的话 需要设置序列化器
    StringRedisTemplate  底层已经UTF-8序列化了

    redis实现锁的方式   获取锁
    this.stringRedisTemplate.opsForValue().setIfAbsent()   不存在 设置
    this.stringRedisTemplate.opsForValue().setIfPresent()  存在设置

手动实现分布式锁：
    1.独占排他：setnx
    2.防死锁发生：客户端应用获取到锁之后，服务器宕机,导致redis中锁无法释放
        （解决: 设置过期时间  指令:set k v ex 30 nx  代码:this.stringRedisTemplate.opsForValue().setIfAbsent("lock","11",30,TimeUnit.SECONDS)   ）
    3.原子性：过期时间和获取锁 以及 判断和删除锁之间 具备原子性（lua脚本）(要么都执行，要么都不执行)
    4.防误删：解铃还须系铃人 , 不能释放别人的锁（判断是否自己的锁）
    5.可重入锁：redis hash（lockname uuid 次数）(可选)
    6.自动续期：看门狗子线程
    7.集群情况下导致锁失效：RedLock
     */


    public List<CategoryEntity> queryLv1lCategories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0l);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }


//+++++根据一级分类id查询显示二级分类携带三级分类++加事务 AOP实现缓存版++++30天43200分钟,随机值7天10080+++++++++++++++++++++
    @GmallCache(prefix = KEY_PREFIX,lock = "lock",timeout = 43200,random = 10080)
    public List<CategoryEntity> queryCategoriesWithSubByPid(Long pid) {

        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();

        return categoryEntities;
    }

//+++++根据一级分类id查询显示二级分类携带三级分类++ 没加事务 AOP实现缓存版++++++++++++++++++++++++++++++++
    //根据一级分类id查询显示二级分类  携带三级分类
    public List<CategoryEntity> queryCategoriesWithSubByPid2(Long pid) {
        //先查询缓存
        String json = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)) {
            //使用阿里的fastjson反序列化工具  反序列化
            return JSON.parseArray(json, CategoryEntity.class);
        }

        //为了防止缓存击穿,添加分布式锁(公平锁getFairLock  非公平锁getLock)
        RLock lock = this.redissonClient.getFairLock("lock");
        lock.lock();
        //再查询缓存,命中则返回
        String json2 = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json2)) {
            //此处防止死锁
            lock.unlock();
            //使用阿里的fastjson反序列化工具  反序列化
            return JSON.parseArray(json2, CategoryEntity.class);
        }

        //没有 再远程查询数据库,序列化并放入redis数据库
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        //为了解决缓存穿透,即使数据不存在,也存进缓存中
        //为了防止缓存雪崩,给缓存时间添加随机值
        //序列化并放入redis数据库,有效期30天+10天随机值
        this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid,JSON.toJSONString(categoryEntities),30+new Random().nextInt(10), TimeUnit.DAYS);

        //释放分布式锁
        lock.unlock();
        return categoryEntities;
    }


//++++++++redisson 分布式锁框架+++++++++++++++++++++++++++++++++++
    // 测试redisson 分布式锁框架
    public void testLock() {
        //加锁
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock(50,TimeUnit.SECONDS);

        String numString = this.stringRedisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(numString)) {
            return;
        }
        Integer num = Integer.parseInt(numString);
        this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));

        testSubLock();
        //解锁
        lock.unlock();
    }

    public void testSubLock() {
        //加锁
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock(50,TimeUnit.SECONDS);

        System.out.println("分布式可重入锁。。。");

        //解锁
        lock.unlock();

    }

//++++++++前四个特点+++++++++++++++++++++++++++++++++++
    // 测试redis 实现分布式锁 使用ab工具测试5000并发
    public void testLock3() {
        // 1.获取锁  特点二:防死锁发生,设置过期时间10秒  特点三:过期时间和获取锁 具备原子性
        String uuid = UUID.randomUUID().toString();
        //特点一:独占排他
        Boolean lock = this.stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 10, TimeUnit.SECONDS);
        //判断
        if (!lock) {
            try {
                //3.没有获取到锁, 重试
                Thread.sleep(300);
                testLock3();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 2.获取锁  执行业务
            String numString = this.stringRedisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                return;
            }
            Integer num = Integer.parseInt(numString);
            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));
            //4.释放锁   特点四:防误删：不能释放别人的锁  特点三:判断和删除锁之间 具备原子性
            //execute 执行脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);


//            判断是否自己的锁
//            if (StringUtils.equals(uuid, this.stringRedisTemplate.opsForValue().get("lock"))) {
//                this.stringRedisTemplate.delete("lock");
//            }

        }
    }
//+++++++测试可重入锁+和+自动续期++++++++++++++++++++++++++++++++++
    // 测试可重入锁         过期时间是long类型的
    public void testLock2() {
        //加锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30l);
        if (!lock) {

        }
        System.out.println("lock = " + lock);
        String numString = this.stringRedisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(numString)) {
            return;
        }
        try {
            TimeUnit.SECONDS.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Integer num = Integer.parseInt(numString);
        this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));

        this.testSubLock2("lock", uuid);

        //解锁
        this.distributedLock.unLock("lock", uuid);

    }

    public void testSubLock2(String LockName,String uuid) {
        //加锁
        this.distributedLock.tryLock(LockName, uuid, 30l);

        System.out.println("分布式可重入锁。。。");

        //解锁
        this.distributedLock.unLock(LockName, uuid);

    }
//++++++++++++++++++++++++++++++++++++++++++++++++






}
