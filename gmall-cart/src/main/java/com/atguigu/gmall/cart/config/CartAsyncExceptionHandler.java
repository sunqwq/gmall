package com.atguigu.gmall.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 自定义异常处理实现类   实现AsyncUncaughtExceptionHandler接口
 *
 * 注意：AsyncUncaughtExceptionHandler 只能拦截  返回类型非Future 的异步调用方法。
 * 返回类型为 Future 的异步调用方法，请使用异步回调来处理。
 */
@Slf4j
@Component
public class CartAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {


    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        log.error("有一个子任务出现了异常.异常信息是:{},异常方法是:{},方法参数是:{}",throwable.getMessage(),method,objects);
    }
}
