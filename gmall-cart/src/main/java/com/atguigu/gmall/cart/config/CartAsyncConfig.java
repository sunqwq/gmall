package com.atguigu.gmall.cart.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 编写配置类, 实现 AsyncConfigurer接口 ，配置自定义异常处理实现类
 */
@Configuration
public class CartAsyncConfig implements AsyncConfigurer {
    @Autowired
    private CartAsyncExceptionHandler cartAsyncExceptionHandler;

    /** 可以给springTask定义专有线程池
     */
    @Override
    public Executor getAsyncExecutor() {
        //return new ThreadPoolExecutor();
        return null;
    }

    /**
     * 注册统一异常处理类
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return cartAsyncExceptionHandler;
    }
}
