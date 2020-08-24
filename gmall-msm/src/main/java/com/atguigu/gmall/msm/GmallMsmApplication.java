package com.atguigu.gmall.msm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@EnableSwagger2
@EnableDiscoveryClient
@ComponentScan("com.atguigu")
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)//取消数据源自动配置
public class GmallMsmApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallMsmApplication.class, args);
    }

}
