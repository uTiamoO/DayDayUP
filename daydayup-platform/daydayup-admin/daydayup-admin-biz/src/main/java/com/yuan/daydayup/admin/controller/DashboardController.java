package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.service.DashboardService;
import com.yuan.daydayup.admin.vo.DashboardStatsVO;
import com.yuan.daydayup.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘接口。
 *
 * <p>任意登录用户均可访问：返回后台核心资源数量概览，用于首页统计卡片。</p>
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public R<DashboardStatsVO> stats() {
        return R.ok(dashboardService.stats());
    }
}
