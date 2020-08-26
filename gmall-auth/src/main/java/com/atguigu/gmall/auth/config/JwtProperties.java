package com.atguigu.gmall.auth.config;


import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * 通过以Properties结尾命名的类中取得在全局配置文件中配置的属性如：server.port，
 * 而XxxxProperties类是通过@ConfigurationProperties注解与全局配置文件中对应的属性进行绑定的。
 * 然后再通过@EnableConfigurationProperties注解导入到Spring容器中。
 */
@ConfigurationProperties(prefix = "auth.jwt")
@Data
@Slf4j
public class JwtProperties {
    //公钥 秘钥对象
    private PublicKey publicKey;
    private PrivateKey privateKey;
    //公钥秘钥路径
    private String pubKeyPath;
    private String priKeyPath;
    //盐
    private String secret;
    // 过期时间
    private Integer expire;
    //cookie名字
    private String cookieName;
    //cookie名称
    private String unick;

    //构造后执行
    @PostConstruct
    public void init() {
        try {
            //初始化公钥秘钥文件对象,来判断存不存在
            File pubFile = new File(pubKeyPath);
            File priFile = new File(priKeyPath);
            //如果不存在,或者不完整,重新生成
            if (!pubFile.exists() || !priFile.exists()) {
                RsaUtils.generateKey(pubKeyPath,priKeyPath,secret);
            }
            //通过文件路径 获取公钥私钥对象
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("生成秘钥和公钥失败,失败原因: " + e.getMessage());
        }

    }


}
