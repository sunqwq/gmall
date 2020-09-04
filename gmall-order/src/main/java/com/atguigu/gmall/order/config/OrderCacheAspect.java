package com.atguigu.gmall.order.config;



import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.util.Arrays;


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
public class OrderCacheAspect {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "order:token:";
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
    @Before("@annotation(com.atguigu.gmall.order.config.OrderCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        // 1.防重
        OrderSubmitVo submitVo = new OrderSubmitVo();
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("^_^");
        }
        // 比较防重标识,相同就删除redis中的记录（lua脚本）
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then return redis.call('del', KEYS[1]) " +
                "else return 0 end";
        Boolean flag = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag){
            throw new OrderException("请不要重复提交！");
        }


        // 2.执行目标方法
        Object result = joinPoint.proceed(joinPoint.getArgs());

        return result;
    }

}
