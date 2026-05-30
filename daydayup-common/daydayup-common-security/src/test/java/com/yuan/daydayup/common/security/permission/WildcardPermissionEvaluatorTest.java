package com.yuan.daydayup.common.security.permission;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WildcardPermissionEvaluatorTest {

    @Test
    void shouldMatchSameAuthority() {
        assertThat(WildcardPermissionEvaluator.hasAuthority(
                List.of(new SimpleGrantedAuthority("admin:user:list")),
                "admin:user:list")).isTrue();
    }

    @Test
    void shouldMatchWildcardAuthority() {
        assertThat(WildcardPermissionEvaluator.hasAuthority(
                List.of(new SimpleGrantedAuthority("admin:*")),
                "admin:user:list")).isTrue();
    }

    @Test
    void shouldRejectDifferentPrefix() {
        assertThat(WildcardPermissionEvaluator.hasAuthority(
                List.of(new SimpleGrantedAuthority("game:*")),
                "admin:user:list")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenAuthoritiesNull() {
        assertThat(WildcardPermissionEvaluator.hasAuthority(null, "admin:user:list")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenRequiredAuthorityNull() {
        assertThat(WildcardPermissionEvaluator.hasAuthority(
                List.of(new SimpleGrantedAuthority("admin:*")), null)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenNoMatch() {
        assertThat(WildcardPermissionEvaluator.hasAuthority(
                List.of(new SimpleGrantedAuthority("game:user:list")),
                "admin:user:list")).isFalse();
    }
}
