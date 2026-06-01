package com.yuan.daydayup.social;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 社交服务启动类
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yuan.daydayup")
@MapperScan("com.yuan.daydayup.social.mapper")
@SpringBootApplication
public class SocialApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }
}
