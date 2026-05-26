package com.yuan.daydayup.common.log.model;

import com.yuan.daydayup.common.log.enums.BusinessType;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志记录
 *
 * <p>由 {@code OperLogAspect} 构造，通过 Spring 事件发布。
 * 业务方实现 {@code ApplicationListener<OperLogRecord>} 自行落库或推送 MQ。</p>
 */
@Data
@Builder
public class OperLogRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 操作标题 */
    private String title;

    /** 业务类型 */
    private BusinessType businessType;

    /** 操作人 ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 请求 URI */
    private String requestUri;

    /** HTTP 方法 */
    private String httpMethod;

    /** 客户端 IP */
    private String clientIp;

    /** 方法签名（类#方法） */
    private String methodSignature;

    /** 请求参数（JSON） */
    private String requestParams;

    /** 响应结果（JSON） */
    private String responseResult;

    /** 是否成功 */
    private boolean success;

    /** 异常信息 */
    private String errorMessage;

    /** 耗时（毫秒） */
    private Long costMs;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 链路 traceId */
    private String traceId;
}
