package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.UserCreateDTO;
import com.yuan.daydayup.admin.dto.UserStatusDTO;
import com.yuan.daydayup.admin.dto.UserUpdateDTO;
import com.yuan.daydayup.admin.entity.SysUser;
import com.yuan.daydayup.admin.mapper.SysUserMapper;
import com.yuan.daydayup.admin.service.impl.UserManageServiceImpl;
import com.yuan.daydayup.admin.vo.UserDetailVO;
import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserManageServiceImplTest {

    private SysUserMapper mapper;
    private PasswordEncoder passwordEncoder;
    private UserManageServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysUserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserManageServiceImpl(mapper, passwordEncoder);
    }

    @Test
    void shouldRejectDuplicateUsername() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");

        when(mapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名已存在");
    }

    @Test
    void shouldUpdateUser() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("newname");
        dto.setNickname("New Nick");

        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setUsername("oldname");
        existing.setStatus(1);

        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.selectById(1L)).thenReturn(existing);
        when(mapper.updateById(any(SysUser.class))).thenReturn(1);

        UserDetailVO vo = service.update(1L, dto);

        assertThat(vo.getUsername()).isEqualTo("newname");
        assertThat(vo.getNickname()).isEqualTo("New Nick");
        verify(mapper).updateById(any(SysUser.class));
    }

    @Test
    void shouldChangeStatus() {
        SysUser existing = new SysUser();
        existing.setId(2L);
        existing.setStatus(1);

        when(mapper.selectById(2L)).thenReturn(existing);
        when(mapper.updateById(any(SysUser.class))).thenReturn(1);

        UserStatusDTO statusDTO = new UserStatusDTO();
        statusDTO.setStatus(0);
        service.changeStatus(2L, statusDTO);

        assertThat(existing.getStatus()).isEqualTo(0);
        verify(mapper).updateById(existing);
    }
}
