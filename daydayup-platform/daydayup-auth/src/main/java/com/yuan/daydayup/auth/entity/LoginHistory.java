package com.yuan.daydayup.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuan.daydayup.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登录历史：用于审计与异常登录检测
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auth_login_history")
public class LoginHistory extends BaseEntity {

    private Long userId;

    private String username;

    private LocalDateTime loginAt;

    private String clientIp;

    private String userAgent;

    private Integer success;

    private String failureReason;
}
