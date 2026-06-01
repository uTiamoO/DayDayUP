package com.yuan.daydayup.game;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 游戏服务启动类
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yuan.daydayup")
@MapperScan("com.yuan.daydayup.game.mapper")
@SpringBootApplication
public class GameApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameApplication.class, args);
    }
}
