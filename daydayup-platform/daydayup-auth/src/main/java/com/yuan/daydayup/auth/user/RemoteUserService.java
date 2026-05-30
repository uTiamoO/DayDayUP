package com.yuan.daydayup.auth.user;

import com.yuan.daydayup.admin.api.feign.UserClient;
import com.yuan.daydayup.admin.api.vo.AuthUserVO;
import com.yuan.daydayup.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 远程用户服务：通过 Feign 调用 admin-biz 获取用户信息
 */
@Service
@RequiredArgsConstructor
public class RemoteUserService {

    private final UserClient userClient;

    public Optional<SimpleUser> findByUsername(String username) {
        R<AuthUserVO> result = userClient.getAuthUserByUsername(username);
        if (result.getData() == null) {
            return Optional.empty();
        }
        AuthUserVO vo = result.getData();
        return Optional.of(SimpleUser.builder()
                .userId(vo.getUserId())
                .username(vo.getUsername())
                .password(vo.getPassword())
                .authorities(vo.getAuthorities())
                .status(vo.getStatus())
                .build());
    }
}
