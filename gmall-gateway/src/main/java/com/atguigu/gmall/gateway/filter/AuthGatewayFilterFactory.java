package com.atguigu.gmall.gateway.filter;


import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *  2.局部过滤器
 *    需要到配置文件中配置
 */

// 2.自定义过滤器工厂中指定Config泛型：KeyValueConfig
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathesConfig> {

    @Autowired
    private JwtProperties jwtProperties;

    //4.通过构造方法指定接收参数的对象类型
    public AuthGatewayFilterFactory() {
        super(PathesConfig.class);
    }

    /**
     * 拦截的业务逻辑
     *
     */
    @Override
    public GatewayFilter apply(PathesConfig config) {
        // 实现GatewayFilter接口
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
             // 1.判断当前路径在不在拦截黑名单中 ,不在直接放行
                // ServerHttpRequest == HttpServletRequest  封装类型不一致,但格式一样,语法一样
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();
                //获取当前路径
                String curPath = request.getURI().getPath();
                //获取黑名单 (配置文件中拦截的路径)
                List<String> pathes = config.pathes;
                System.out.println(" 这是自定义1 " + pathes);
                //如果当前路径不在黑名单中 ,直接放行
                if (pathes.stream().allMatch(path -> curPath.indexOf(path) == -1)) {
                    return chain.filter(exchange);
                }
                System.out.println(" 这是自定义2 ");
             // 2. 获取 头信息 或者 cookie 中的token信息 (头信息中只会有一个token  contains包含)
                String token = request.getHeaders().getFirst("token");
                if (StringUtils.isBlank(token)) {
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())) {
                        token = cookies.getFirst(jwtProperties.getCookieName()).getValue();
                    }
                }

             // 3.判断token是否为空 ,空则拦截(重定向到登录页面)
                if (StringUtils.isBlank(token)) {
                    return interceptor(request,response);
                }

                try {
                    // 4.解析jwt类型的token获取用户信息
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());

                    // 5.判断当前用户的ip是否和token中的ip一致(防止被盗用)
                    String ip = map.get("ip").toString();
                    String curIp = IpUtil.getIpAddressAtGateway(request);
                    if (!StringUtils.equals(ip, curIp)) {
                        // 不一致说明被盗用，直接拦截
                        return interceptor(request,response);
                    }
                    // 6.把解析后的用户信息传递给后续服务
                    String userId = map.get("userId").toString();
                    String username = map.get("username").toString();
                    // 把登录信息放入 request头信息中传递给后续服务  (mutate转化)
                      // 返回新的request
                    request = request.mutate().header("userId", userId).header("userName", username).build();
                      // 返回新的exchange
                    exchange = exchange.mutate().request(request).build();
                    System.out.println(" 这是自定义3 ");
                } catch (Exception e) {
                    e.printStackTrace();
                    // 报错，直接拦截
                    return interceptor(request,response);
                }
                // 7. 放行
                return chain.filter(exchange);
            }
        };
    }

    //空 则拦截(重定向到登录页面)
    private Mono<Void> interceptor(ServerHttpRequest request, ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.SEE_OTHER);
        response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin?returnUrl=" + request.getURI());
        // 请求结束返回Mono<void>
        return response.setComplete();
    }

    //3.通过shortcutFieldOrder方法指定接收参数的属性顺序
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("pathes");
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    //1.自定义config内部类（添加接收参数的字段） KeyValueConfig
    @Data
    @ToString
    public static class PathesConfig{
        private List<String> pathes;
    }


}
