package com.yuan.daydayup.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 调度中心启动类
 *
 * <p>说明：本模块作为业务侧调度对接的占位。生产部署推荐直接使用 XXL-JOB 官方
 * Admin 镜像（{@code xuxueli/xxl-job-admin}），业务服务通过 {@code xxl-job-core}
 * 作为执行器接入。</p>
 */
@EnableDiscoveryClient
@SpringBootApplication
public class JobApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobApplication.class, args);
    }
}
