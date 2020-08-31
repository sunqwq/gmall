package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.common.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 自定义异常处理实现类   实现AsyncUncaughtExceptionHandler接口
 *
 * 注意：AsyncUncaughtExceptionHandler 只能拦截  返回类型非Future 的异步调用方法。
 * 返回类型为 Future 的异步调用方法，请使用异步回调来处理。
 *
 *
 * 发生异常的购物车都在这放进redis中 Map<key , List()>
 *     由于购物车采用的是同步操作redis，异步操作mysql。即使mysql操作失败，只要redis操作成功，依然不影响功能的使用。
 *     但是，如果不进行定期的数据同步处理，则可能导致mysql数据存在严重偏差，失去了数据分析价值
 *     配合gmall-scheduled 中的xxl-job 定时同步购物车数据。
 */
@Slf4j
@Component
public class CartAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String key = "cart:async:exception";

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        log.error("有一个子任务出现了异常.异常信息是:{},异常方法是:{},方法参数是:{}",throwable.getMessage(),method,objects);

        // 把异常用户信息存入redis
        String userId = objects[0].toString();
        if (StringUtils.isNotBlank(userId)) {

//            BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(key);
//            // 从左边放入
//            listOps.leftPush(userId);

            BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(key);
            // 从左边放入
            setOps.add(userId);
        } else {
            throw new UserException("该用户的购物车异步执行失败,并且没有传递用户信息! ");
        }
    }
}
