package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CookieValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * @EnableConfigurationProperties 启动属性读取类 读取配置文件中的内容
 */

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {
    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     */
    public void accredit(String loginName, String password, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 1. 远程调用接口,查询用户名和密码是否正确
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUser(loginName, password);
        UserEntity userEntity = userEntityResponseVo.getData();

        // 2.判断用户是否为空
        if (userEntity == null) {
            throw new UserException("该用户不存在");
        }
        // 3.生成jwt
        //防止被盗用jwt,有效载荷中加入了用户的ip地址
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId",userEntity.getId());
        map.put("username",userEntity.getUsername());
        map.put("ip", IpUtil.getIpAddressAtService(request));
        String jwt = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());

        // 4.把jwt类型的token放入cookie中
        CookieUtils.setCookie(request, response, jwtProperties.getCookieName(), jwt, jwtProperties.getExpire() * 60);
        // 5用户昵称
        CookieUtils.setCookie(request,response,jwtProperties.getUnick(),userEntity.getNickname(),jwtProperties.getExpire());

    }
}
