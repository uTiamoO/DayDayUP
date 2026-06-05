package com.yuan.daydayup.auth.grant;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// password grant 已从 admin-web 种子客户端下线（遵循设计文档：public client 不启用 password grant）。
// PasswordAuthenticationProvider 链路保留备用；如需重新启用本测试，需配套一个支持 password grant 的 confidential client。
@Disabled("password grant 已下线（admin-web 不再支持）；且需 MySQL + Redis 基础设施")
@SpringBootTest
@AutoConfigureMockMvc
class PasswordGrantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void passwordGrantReturnsAccessToken() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic("admin-web", ""))  // public client, no secret
                        .param("grant_type", "password")
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").isNumber());
    }

    @Test
    void passwordGrantWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .with(httpBasic("admin-web", ""))
                        .param("grant_type", "password")
                        .param("username", "admin")
                        .param("password", "wrong"))
                .andExpect(status().isUnauthorized());
    }
}
