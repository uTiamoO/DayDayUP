package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.vo.DashboardStatsVO;

/**
 * 仪表盘统计服务接口。
 */
public interface DashboardService {

    /** 聚合后台核心资源数量（用户 / 角色 / 菜单 / 字典）。 */
    DashboardStatsVO stats();
}
