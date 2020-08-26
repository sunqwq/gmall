package com.atguigu.gmall.gateway.config;


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
 *
 * @ConfigurationProperties 报错的原因是还没启用, 启用即可@EnableConfigurationProperties
 */
@ConfigurationProperties(prefix = "auth.jwt")
@Data
@Slf4j
public class JwtProperties {
    //公钥 对象
    private PublicKey publicKey;

    //公钥路径
    private String pubKeyPath;
    //cookie名字
    private String cookieName;


    //构造后执行
    @PostConstruct
    public void init() {
        try {
            //通过文件路径 获取公钥对象
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("生成秘钥和公钥失败,失败原因: " + e.getMessage());
        }

    }


}
