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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserManageServiceImplTest {

    private SysUserMapper mapper;
    private PasswordEncoder passwordEncoder;
    private com.yuan.daydayup.common.redis.util.CacheService cacheService;
    private PermissionVersionService permissionVersionService;
    private UserManageServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysUserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        cacheService = mock(com.yuan.daydayup.common.redis.util.CacheService.class);
        permissionVersionService = mock(PermissionVersionService.class);
        service = new UserManageServiceImpl(mapper, passwordEncoder, cacheService, permissionVersionService);
    }

    @Test
    void shouldRejectDuplicateUsername() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");

        when(mapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用户名已存在");
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

    @Test
    void shouldClearCacheOnStatusChange() {
        SysUser existing = new SysUser();
        existing.setId(2L);
        existing.setStatus(1);

        when(mapper.selectById(2L)).thenReturn(existing);
        when(mapper.updateById(any(SysUser.class))).thenReturn(1);

        UserStatusDTO statusDTO = new UserStatusDTO();
        statusDTO.setStatus(0);
        service.changeStatus(2L, statusDTO);

        // 验证用户状态变更时清除缓存
        verify(cacheService).delete("daydayup:admin:user:2");
    }

    @Test
    void shouldCacheNullForNonExistingUserToPreventPenetration() {
        when(mapper.selectById(999L)).thenReturn(null);

        // 首次查询：直接查 DB，应当写入空值缓存以防穿透
        UserDetailVO vo1 = service.detail(999L);
        assertThat(vo1).isNull();
        verify(cacheService).setJson(
                org.mockito.ArgumentMatchers.eq("daydayup:admin:user:999"),
                any(UserDetailVO.class),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(java.util.concurrent.TimeUnit.MINUTES)
        );

        // 再次查询：应当命中缓存的空值且不再访问 DB
        UserDetailVO emptyVo = new UserDetailVO();
        when(cacheService.getJson("daydayup:admin:user:999", UserDetailVO.class)).thenReturn(emptyVo);

        UserDetailVO vo2 = service.detail(999L);
        assertThat(vo2).isNull();
        // 验证除了之前的 selectById 之外，没有更多 mapper 交互
        verify(mapper, org.mockito.Mockito.times(1)).selectById(999L);
    }
}
