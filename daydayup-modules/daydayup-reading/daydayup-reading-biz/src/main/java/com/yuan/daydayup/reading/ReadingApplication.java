package com.yuan.daydayup.reading;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 阅读中台服务启动类
 *
 * <p>以开源阅读（Legado）书源 JSON 为输入，编译为内部 RuleModel，经解析运行时抓取
 * 搜索/详情/目录/正文，再经净化 Pipeline 生产并持久化正文，最终对外提供统一阅读 API。</p>
 *
 * <p>内部按 package 强边界拆分：source / compiler / runtime / pipeline / repository / api / ops / task。</p>
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yuan.daydayup")
// 按 @Mapper 注解过滤，避免把 service/其它接口误注册为 Mapper（包按领域拆分，非扁平 mapper 包）
@MapperScan(basePackages = "com.yuan.daydayup.reading", annotationClass = Mapper.class)
@SpringBootApplication
public class ReadingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReadingApplication.class, args);
    }
}
