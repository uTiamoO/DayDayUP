package com.yuan.daydayup.admin.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OperLogVO {
    private Long id;
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
