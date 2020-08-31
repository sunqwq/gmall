package com.atguigu.gmall.order.config;

import com.atguigu.gmall.common.exception.OrderException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.annotation.*;
import java.util.Arrays;

//注解类

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OrderCache {


}
