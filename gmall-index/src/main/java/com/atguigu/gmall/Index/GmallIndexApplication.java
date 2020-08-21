package com.atguigu.gmall.Index;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@EnableFeignClients
@SpringBootApplication
public class GmallIndexApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallIndexApplication.class, args);
    }

}
