package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.entity.LoginHistory;
import com.yuan.daydayup.auth.mapper.LoginHistoryMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryMapper loginHistoryMapper;

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
