package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.user.RemoteUserService;
import com.yuan.daydayup.auth.user.SimpleUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * SAS 认证用的 UserDetailsService。
 *
 * <p>Phase 1：委托给 {@link RemoteUserService}（Feign 查 admin DB）。</p>
 * <p>Phase 2：身份数据迁入本地后，改为注入 {@code SysUserMapper} 直接查询。</p>
 */
@Service
@RequiredArgsConstructor
public class DayDayUpUserDetailsService implements UserDetailsService {

    private final RemoteUserService remoteUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SimpleUser simpleUser = remoteUserService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Collection<GrantedAuthority> authorities = simpleUser.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        boolean enabled = simpleUser.getStatus() != null && simpleUser.getStatus() == 1;

        return User.builder()
                .username(simpleUser.getUsername())
                .password(simpleUser.getPassword())
                .disabled(!enabled)
                .authorities(authorities)
                .build();
    }
}
