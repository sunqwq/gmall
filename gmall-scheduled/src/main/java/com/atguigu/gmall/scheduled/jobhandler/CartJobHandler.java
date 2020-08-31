package com.atguigu.gmall.scheduled.jobhandler;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.atguigu.gmall.scheduled.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * XxlJob开发示例（Bean模式）
 *
 * 开发步骤：
 * 1、在Spring Bean实例中，开发Job方法，方式格式要求为 "public ReturnT<String> execute(String param)"
 * 2、为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobLogger.log" 打印执行日志；
 *
 *  map<key , list<> >
 *  map<key , map<key,value> >
 *  原本应该远程调用cart （根据用户id同步用户数据的接口）, 这里为了省事直接复制mapper,Cart
 */

@Component
public class CartJobHandler {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartMapper cartMapper;

    private static final String key = "cart:async:exception";

    private static final String KEY_PREFIX = "cart:info:";

    @XxlJob("cartJobHandler")
    public ReturnT<String> executor(String param) {

        // 读取失败用户信息 (循环读取)
//        BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(key);
//        String userId = listOps.rightPop();

        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(key);
        // 如果redis中出现异常的用户为空，则直接返回
        if (setOps.size() == 0){
            return ReturnT.SUCCESS;
        }
        // 获取第一个失败的用户
        String userId = setOps.pop();

        while (StringUtils.isNotBlank(userId)) {
            // 先删除mysql中该用户的购物车
            this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));
            // 读取redis中该用户的购物车信息
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartJsons = hashOps.values();
            // 如果该用户购物车数据为空，则直接进入下次循环
            if (CollectionUtils.isEmpty(cartJsons)) {
                userId = setOps.pop();
                continue;
            }

            // 最后，如果不为空，再新增mysql中该用户的购物车
            cartJsons.forEach(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                this.cartMapper.insert(cart);
            });

            // 下一个用户
            userId = setOps.pop();
        }
        return ReturnT.SUCCESS;
    }

}
