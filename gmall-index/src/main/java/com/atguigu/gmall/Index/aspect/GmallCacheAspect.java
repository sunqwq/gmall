package com.atguigu.gmall.Index.aspect;


import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/** springAOP
 * 声明一个环绕通知
 *      1.方法的返回值必须是Object
 * 		2.方法形参中必须有一个ProceedingJoinPoint参数（JoinPoint）
 * 		3.方法必须抛出一个Throwable异常
 * 		4.proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs())执行目标方法
 *
 */
@Aspect
@Component
public class GmallCacheAspect {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;

/*
    环绕通知 基本结构
    @Around()
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        Object result = joinPoint.proceed(joinPoint.getArgs());
        return result;
    }
*/
    //@Around拦截GmallCache注解
    /**
     * 获取目标对象类：joinPoint.getTarget().getClass()
     * 获取目标方法参数：joinPoint.getArgs()
     *  获取方法签名: (MethodSignature) joinPoint.getSignature()
     * 获取目标方法：(MethodSignature)joinPoint.getSignature().getMethod()
     * 获取目标方法的返回值类型:(MethodSignature)joinPoint.getSignature().getReturnType()
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.atguigu.gmall.Index.aspect.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        //获取方法参数 ,数组的toString方法返回的是地址,没有可读性,所以后面 用集合
        Object[] args = joinPoint.getArgs();
        //获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取方法名
        Method method = signature.getMethod();
        //获取注解中的key的前缀,锁名,过期时间,随机值
        GmallCache annotation = method.getAnnotation(GmallCache.class);
        String prefix = annotation.prefix();
        String lock = annotation.lock();
        int timeout = annotation.timeout();
        int random = annotation.random();
        //获取返回值类型
        Class returnType = signature.getReturnType();
        //获取key值,前缀+参数
        String key = prefix+ Arrays.asList(args);

        // 1.查询缓存,如果命中直接反序列化返回
        String json = this.stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseObject(json,returnType);
        }
        // 2.加分布式锁  公平锁  (这里修改后,没有测试,如果失败,换用下一个)
        RLock fairLock = this.redissonClient.getFairLock(lock + Arrays.asList(args));
        //RLock fairLock = this.redissonClient.getFairLock(lock + args);
        fairLock.lock();

        // 3.查询缓存,如果命中直接反序列化返回
        String json2 = this.stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json2)) {
            //此处需要注意释放锁,防止死锁
            fairLock.unlock();
            return JSON.parseObject(json2,returnType);
        }

        // 4.执行目标方法
        Object result = joinPoint.proceed(joinPoint.getArgs());

        // 5.将返回值 序列化 放进缓存
        this.stringRedisTemplate.opsForValue().set(key,JSON.toJSONString(result),timeout+new Random().nextInt(random), TimeUnit.MINUTES);

        // 6.释放锁
        fairLock.unlock();
        return result;
    }

}
