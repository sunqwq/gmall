package com.atguigu.gmall.gateway.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 解决跨域 配置类
 *
 *
 * Spring已经帮我们写好了CORS的跨域过滤器，内部已经实现了刚才所讲的判定逻辑。
 *
 * spring-webmvc：CorsFilter
 * spring-webflux：CorsWebFilter
 *
 * springcloud-gateway集成的是webflux，所以这里使用的是CorsWebFilter
 */

@Configuration   //配置类注解
public class CorsWebConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        // 初始化CORS配置对象
        CorsConfiguration config = new CorsConfiguration();
        /*
        如果需要服务器允许跨域，需要在返回的响应头中携带下面信息
Access-Control-Allow-Origin：可接受的域，是一个具体域名或者*（代表任意域名,不要写*，否则cookie就无法使用了）
Access-Control-Allow-Credentials：是否允许携带cookie，默认情况下，cors不会携带cookie，除非这个值是true
Access-Control-Allow-Methods：允许访问的方式
Access-Control-Allow-Headers：允许携带的头
Access-Control-Max-Age：本次许可的有效时长，单位是秒，**过期之前的ajax请求就无需再次进行预检了**
        */

        // 1.允许跨域访问的域名,  不要写*，否则cookie就无法使用了
        config.addAllowedOrigin("http://manager.gmall.com");
        config.addAllowedOrigin("http://www.gmall.com");
        // 2.允许携带的头信息
        config.addAllowedHeader("*");
        // 3.允许的请求方式
        config.addAllowedMethod("*");
        // 4.是否允许携带Cookie信息
        config.setAllowCredentials(true);

        // 添加映射路径，我们拦截一切请求
        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();

        corsConfigurationSource.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(corsConfigurationSource);
    }

}
