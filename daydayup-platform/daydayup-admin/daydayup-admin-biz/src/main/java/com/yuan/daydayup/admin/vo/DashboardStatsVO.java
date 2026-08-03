package com.yuan.daydayup.admin.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 仪表盘统计视图对象。
 *
 * <p>聚合后台核心资源的数量，用于首页概览卡片展示。</p>
 */
@Data
@Builder
public class DashboardStatsVO {

    /** 用户总数（经 auth 服务分页总数获取）。 */
    private Long userCount;

    /** 角色总数（经 auth 服务分页总数获取）。 */
    private Long roleCount;

    /** 菜单总数（本地统计）。 */
    private Long menuCount;

    /** 字典总数（本地统计）。 */
    private Long dictCount;
}
