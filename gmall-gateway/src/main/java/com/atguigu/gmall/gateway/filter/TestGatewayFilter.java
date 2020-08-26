package com.atguigu.gmall.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关过滤器验证登录状态
 *
 * 1. 自定义全局过滤器 (只要经过就会被拦截)
 */
//@Order(1)
@Component
public class TestGatewayFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        System.out.println("这是全局过滤器,无需配置,拦截所有经过网关的请求！！");
        // 放行
        return chain.filter(exchange);
    }

    /** 或者使用@Order注解
     * 通过实现Orderer接口的getOrder方法控制全局过滤器的执行顺序
     * 返回值越小 ,优先值越高
     */
    @Override
    public int getOrder() {
        return 1;
    }
}
