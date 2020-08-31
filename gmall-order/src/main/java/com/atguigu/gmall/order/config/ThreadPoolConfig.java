package com.atguigu.gmall.order.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 手写 线程池
 *
 * 此处的线程工厂,拒绝策略使用的默认的
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(@Value("${threadPool.coreSize}")Integer coreSize,
                                                 @Value("${threadPool.maxSiza}")Integer maxSiza,
                                                 @Value("${threadPool.timeOut}")Integer timeOut,
                                                 @Value("${threadPool.blockingSize}")Integer blockingSize) {


        return new ThreadPoolExecutor(coreSize,maxSiza,timeOut, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(blockingSize));
    }


}
