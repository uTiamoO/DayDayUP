package com.yuan.daydayup.auth.user;

import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 内存用户存储（P1 占位，已被 {@link RemoteUserService} 替代，保留作为本地调试参考）
 *
 * <p>初始内置两个账号：</p>
 * <ul>
 *   <li>{@code admin / admin}，拥有 {@code admin:*}、{@code game:*}、{@code social:*}</li>
 *   <li>{@code user  / user}，拥有 {@code game:play}、{@code social:read}</li>
 * </ul>
 */
public class InMemoryUserService {

    private final Map<String, SimpleUser> users = new HashMap<>();

    private final PasswordEncoder passwordEncoder;

    public InMemoryUserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        users.put("admin", SimpleUser.builder()
                .userId(1L)
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .authorities(Set.of("admin:*", "game:*", "social:*"))
                .build());

        users.put("user", SimpleUser.builder()
                .userId(2L)
                .username("user")
                .password(passwordEncoder.encode("user"))
                .authorities(Set.of("game:play", "social:read"))
                .build());
    }

    public Optional<SimpleUser> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
