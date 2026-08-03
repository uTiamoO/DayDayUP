package com.yuan.daydayup.admin.service.impl;

import com.yuan.daydayup.admin.mapper.SysDictMapper;
import com.yuan.daydayup.admin.mapper.SysMenuMapper;
import com.yuan.daydayup.admin.service.DashboardService;
import com.yuan.daydayup.admin.service.RoleService;
import com.yuan.daydayup.admin.service.UserManageService;
import com.yuan.daydayup.admin.vo.DashboardStatsVO;
import com.yuan.daydayup.auth.api.dto.RolePageQuery;
import com.yuan.daydayup.auth.api.dto.UserPageQuery;
import com.yuan.daydayup.common.core.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 仪表盘统计服务实现。
 *
 * <p>菜单 / 字典为 admin-biz 本地表，直接通过 Mapper 统计；用户 / 角色由 auth 服务
 * 拥有，借助分页接口（pageSize=1）读取总数，避免拉取整表数据。</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SysMenuMapper menuMapper;
    private final SysDictMapper dictMapper;
    private final UserManageService userManageService;
    private final RoleService roleService;

    @Override
    public DashboardStatsVO stats() {
        return DashboardStatsVO.builder()
                .userCount(countUsers())
                .roleCount(countRoles())
                .menuCount(menuMapper.selectCount(null))
                .dictCount(dictMapper.selectCount(null))
                .build();
    }

    private Long countUsers() {
        UserPageQuery query = new UserPageQuery();
        query.setPageNum(1L);
        query.setPageSize(1L);
        PageResult<?> page = userManageService.page(query);
        return page == null || page.getTotal() == null ? 0L : page.getTotal();
    }

    private Long countRoles() {
        RolePageQuery query = new RolePageQuery();
        query.setPageNum(1L);
        query.setPageSize(1L);
        PageResult<?> page = roleService.page(query);
        return page == null || page.getTotal() == null ? 0L : page.getTotal();
    }
}
