package com.yuan.daydayup.auth.service;

import com.yuan.daydayup.auth.user.SimpleUser;
import com.yuan.daydayup.auth.user.RemoteUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DayDayUpUserDetailsServiceTest {

    private final RemoteUserService remoteUserService = mock(RemoteUserService.class);
    private final DayDayUpUserDetailsService service = new DayDayUpUserDetailsService(remoteUserService);

    @Test
    void loadUserByUsername_found() {
        SimpleUser simpleUser = SimpleUser.builder()
                .userId(1L).username("admin").password("$2a$10$hash")
                .authorities(Set.of("admin:*")).status(1).build();
        when(remoteUserService.findByUsername("admin")).thenReturn(Optional.of(simpleUser));

        UserDetails result = service.loadUserByUsername("admin");

        assertEquals("admin", result.getUsername());
        assertEquals("$2a$10$hash", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("admin:*")));
        assertTrue(result.isEnabled());
    }

    @Test
    void loadUserByUsername_notFound() {
        when(remoteUserService.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost"));
    }

    @Test
    void loadUserByUsername_disabled() {
        SimpleUser disabled = SimpleUser.builder()
                .userId(2L).username("disabled").password("$2a$10$hash")
                .authorities(Set.of()).status(0).build();
        when(remoteUserService.findByUsername("disabled")).thenReturn(Optional.of(disabled));

        UserDetails result = service.loadUserByUsername("disabled");
        assertFalse(result.isEnabled());
    }
}
