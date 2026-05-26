package com.yuan.daydayup.common.log.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuan.daydayup.common.core.constant.CommonConstants;
import com.yuan.daydayup.common.core.context.UserContext;
import com.yuan.daydayup.common.core.context.UserContextHolder;
import com.yuan.daydayup.common.log.annotation.OperLog;
import com.yuan.daydayup.common.log.model.OperLogRecord;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 *
 * <p>切面捕获 {@link OperLog} 标注的方法，构造 {@link OperLogRecord} 后通过 Spring 事件机制发布。
 * 业务方实现 {@code @EventListener} 监听异步落库，与切面解耦。</p>
 */
@Slf4j
@Aspect
public class OperLogAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OperLogAspect(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operLog) || @within(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperLogRecord.OperLogRecordBuilder builder = OperLogRecord.builder()
                .title(operLog.title())
                .businessType(operLog.businessType())
                .operateTime(LocalDateTime.now())
                .methodSignature(formatMethod(joinPoint));

        fillUser(builder);
        fillRequest(builder, operLog, joinPoint);

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            builder.costMs(System.currentTimeMillis() - start);
            builder.success(error == null);
            if (error != null) {
                builder.errorMessage(error.getMessage());
            } else if (operLog.recordResult() && result != null) {
                builder.responseResult(safeToJson(result));
            }
            try {
                eventPublisher.publishEvent(builder.build());
            } catch (RuntimeException ex) {
                log.warn("操作日志发布失败：{}", ex.getMessage());
            }
        }
    }

    private void fillUser(OperLogRecord.OperLogRecordBuilder builder) {
        UserContext context = UserContextHolder.get();
        if (context != null) {
            builder.operatorId(context.getUserId());
            builder.operatorName(context.getUsername());
        }
    }

    private void fillRequest(OperLogRecord.OperLogRecordBuilder builder,
                             OperLog operLog,
                             ProceedingJoinPoint joinPoint) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            builder.requestUri(request.getRequestURI());
            builder.httpMethod(request.getMethod());
            builder.clientIp(resolveClientIp(request));
            builder.traceId(request.getHeader(CommonConstants.HEADER_TRACE_ID));
        }
        if (operLog.recordParams()) {
            builder.requestParams(safeToJson(joinPoint.getArgs()));
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String formatMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
    }

    private String safeToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (obj.getClass().isArray()) {
                return objectMapper.writeValueAsString(Arrays.asList((Object[]) obj));
            }
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            return "[unserializable: " + ex.getOriginalMessage() + "]";
        }
    }
}
