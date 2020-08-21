package com.atguigu.gmall.Index.aspect;

import java.lang.annotation.*;

//注解类

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存key 的前缀
     * @return
     */
    String prefix() default "";

    /**防止缓存穿透,需添加分布式锁
        此处指定锁的名称
     */
    String lock() default "lock";

    /**
     * 缓存过期时间,单位分钟
     */
    int timeout() default 5;

    /**
     * 为了防止缓存雪崩,可以指定随机值范围,单位分钟
     */
    int random() default 5;


}
