package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.entity.LoginHistory;
import com.yuan.daydayup.auth.mapper.LoginHistoryMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录历史服务
 *
 * <p>记录每一次登录尝试（成功与失败）用于安全审计与异常登录检测。</p>
 */
@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryMapper loginHistoryMapper;

    /**
     * 写入一条登录历史。
     *
     * @param userId        用户 ID；用户不存在等场景下可为 null
     * @param username      登录用户名
     * @param success       是否登录成功
     * @param failureReason 失败原因（如 LOGIN_LOCKED / USER_NOT_FOUND / BAD_PASSWORD），成功时为 null
     * @param request       当前 HTTP 请求，用于提取客户端 IP 与 User-Agent
     */
    public void record(Long userId, String username, boolean success,
                       String failureReason, HttpServletRequest request) {
        LoginHistory history = new LoginHistory();
        history.setUserId(userId);
        history.setUsername(username);
        history.setLoginAt(LocalDateTime.now());
        history.setClientIp(getClientIp(request));
        history.setUserAgent(request.getHeader("User-Agent"));
        history.setSuccess(success ? 1 : 0);
        history.setFailureReason(failureReason);
        loginHistoryMapper.insert(history);
    }

    /**
     * 解析客户端真实 IP：优先取反向代理头 X-Forwarded-For 的首个地址，
     * 其次 X-Real-IP，最后回退到 {@link HttpServletRequest#getRemoteAddr()}。
     *
     * @param request 当前 HTTP 请求
     * @return 客户端 IP
     */
    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
