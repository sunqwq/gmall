package com.atguigu.gmall.cart.Interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

/**
 * HandlerInterceptor 自定义拦截器 (SpringMVC)
 *
 * 使用拦截器 登录校验   获取userId或userKey
 */

//启用JwtProperties

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtProperties jwtProperties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();


    /**
     * 前置方法 , 目标方法之前的操作
     *   返回true 表示可以访问目标资源
     *   返回false 表示不允许访问目标资源
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取cookie中token信息以及userKey信息
        String token = CookieUtils.getCookieValue(request, this.jwtProperties.getCookieName());
        String userKey = CookieUtils.getCookieValue(request, this.jwtProperties.getUserKeyName());
        //如果 userKey 为空,生成一个放入cookie中,有效期半年
        if (StringUtils.isBlank(userKey)) {
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request, response, this.jwtProperties.getUserKeyName(), userKey, 180 * 24 * 3600);
        }

        //token不为空,则解析出用户信息 userId
        Long userId = null;
        if (StringUtils.isNotBlank(token)) {
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, this.jwtProperties.getPublicKey());
            userId = new Long(map.get("userId").toString()) ;
        }
        //TODO 怎么把登录信息传递给后续的业务流程
        /**
         * 1.public类型成员变量传递（线程不安全）：单例模式 + 状态字段
         * 	解决方案：使用多例模式@Scope("prototype")或者不要使用状态字段 ,但是再spring中不推荐使用多例,因为使用多了会产生OOM
         *          为什么controller ,service没有线程安全问题,因为它没有状态字段(用户信息,身高,体重)
         * 2.request域：不够优雅
         * 3.ThreadLocal 线程变量。 推荐
         */
//        request.setAttribute("userId", userId);
//        request.setAttribute("userKey", userKey);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setUserKey(userKey);
        THREAD_LOCAL.set(userInfo);

        return true;
    }

    //提供方法来获取userInfo
    public static UserInfo getUserInfo() {
        return THREAD_LOCAL.get();
    }

    /**
     * 页面渲染完成后执行
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 必须手动释放线程局部变量，否则会导致内存泄漏
        // 因为使用的是线程池，请求结束线程没有结束，导致内存无法自动释放
        THREAD_LOCAL.remove();
    }
}
