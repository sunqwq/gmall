package com.atguigu.gmall.payment.Interceptor;


import com.atguigu.gmall.common.bean.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * HandlerInterceptor 自定义拦截器 (SpringMVC)
 *
 * 使用拦截器 登录校验   获取userId (到这一步肯定登录了)
 */


@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 前置方法 , 目标方法之前的操作
     *   返回true 表示可以访问目标资源
     *   返回false 表示不允许访问目标资源
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String userId = request.getHeader("userId");
        if (StringUtils.isNotBlank(userId)) {
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(Long.valueOf(userId));
            THREAD_LOCAL.set(userInfo);
        }

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
