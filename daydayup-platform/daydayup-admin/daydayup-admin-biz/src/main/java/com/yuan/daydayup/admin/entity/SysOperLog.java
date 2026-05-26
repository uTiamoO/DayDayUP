package com.yuan.daydayup.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    private Long userId;

    private String username;

    private String module;

    private String operation;

    private String method;

    private String requestUrl;

    private String requestMethod;

    private String requestIp;

    private String requestParams;

    private String responseBody;

    private Integer success;

    private String errorMsg;

    private Long costMs;

    private LocalDateTime operTime;
}
