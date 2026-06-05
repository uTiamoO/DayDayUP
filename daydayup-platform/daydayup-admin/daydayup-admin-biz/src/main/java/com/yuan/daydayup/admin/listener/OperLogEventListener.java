package com.yuan.daydayup.admin.listener;

import com.yuan.daydayup.admin.entity.SysOperLog;
import com.yuan.daydayup.admin.mapper.SysOperLogMapper;
import com.yuan.daydayup.common.log.model.OperLogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志落库监听器
 *
 * <p>接收 {@link OperLogRecord} 事件，转换后写入 sys_oper_log 表。
 * 由 {@code OperLogAutoConfiguration} 中的 {@code @EnableAsync} 支撑异步执行，
 * 不阻塞业务请求线程。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperLogEventListener {

    private final SysOperLogMapper operLogMapper;

    @Async("logExecutor")
    @EventListener
    public void onOperLog(OperLogRecord record) {
        try {
            SysOperLog entity = toEntity(record);
            operLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("操作日志落库失败: uri={}, error={}", record.getRequestUri(), e.getMessage(), e);
        }
    }

    private SysOperLog toEntity(OperLogRecord record) {
        SysOperLog entity = new SysOperLog();
        entity.setUserId(record.getOperatorId());
        entity.setUsername(record.getOperatorName());
        entity.setModule(extractModule(record.getRequestUri()));
        entity.setOperation(record.getTitle());
        entity.setMethod(record.getMethodSignature());
        entity.setRequestUrl(record.getRequestUri());
        entity.setRequestMethod(record.getHttpMethod());
        entity.setRequestIp(record.getClientIp());
        entity.setRequestParams(record.getRequestParams());
        entity.setResponseBody(record.getResponseResult());
        entity.setSuccess(record.isSuccess() ? 1 : 0);
        entity.setErrorMsg(record.getErrorMessage());
        entity.setCostMs(record.getCostMs());
        entity.setOperTime(record.getOperateTime());
        return entity;
    }

    /**
     * 从请求 URI 提取一级路径作为模块名，例如 /admin/users → admin
     */
    private static String extractModule(String uri) {
        if (uri == null || uri.length() < 2) {
            return "unknown";
        }
        // 跳过开头的 '/'，取第一段路径
        String trimmed = uri.startsWith("/") ? uri.substring(1) : uri;
        int idx = trimmed.indexOf('/');
        return idx > 0 ? trimmed.substring(0, idx) : trimmed;
    }
}
