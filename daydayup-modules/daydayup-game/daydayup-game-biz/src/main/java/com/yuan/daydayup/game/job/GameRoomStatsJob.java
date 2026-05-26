package com.yuan.daydayup.game.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 游戏服务示例定时任务
 *
 * <p>在 XXL-JOB Admin 中创建任务时填写 {@code JobHandler = gameRoomStatsJob}。</p>
 */
@Slf4j
@Component
public class GameRoomStatsJob {

    @XxlJob("gameRoomStatsJob")
    public void execute() {
        log.info("[xxl-job] 游戏房间统计任务执行：统计在线房间数 / 玩家分布 / 高峰时段...");
    }
}
