package com.yuan.daydayup.auth.user;

import com.yuan.daydayup.admin.api.feign.UserClient;
import com.yuan.daydayup.admin.api.vo.AuthUserVO;
import com.yuan.daydayup.common.core.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 远程用户服务：通过 Feign 调用 admin-biz 获取 / 更新用户信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteUserService {

    private final UserClient userClient;

    /**
     * 按用户名查询认证用户信息（含密码哈希、权限、状态）。
     *
     * @param username 用户名
     * @return 用户信息；不存在时返回 {@link Optional#empty()}
     */
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

    /**
     * 回写用户最后登录信息（IP / 时间）。
     *
     * <p>失败不应阻断登录主流程，仅记录告警。</p>
     *
     * @param userId   用户 ID
     * @param clientIp 客户端 IP
     */
    public void updateLoginInfo(Long userId, String clientIp) {
        try {
            userClient.updateLoginInfo(userId, clientIp);
        } catch (Exception e) {
            log.warn("更新用户[{}]最后登录信息失败：{}", userId, e.getMessage());
        }
    }
}
