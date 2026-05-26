package com.yuan.daydayup.game.controller;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.game.api.feign.GameClient;
import com.yuan.daydayup.game.api.vo.GameRoomVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 游戏房间接口（P2 阶段 mock 数据）
 *
 * <p>同时实现 {@link GameClient}，确保 Controller 路径与 Feign 契约严格一致。</p>
 */
@RestController
@Tag(name = "游戏房间")
public class GameRoomController implements GameClient {

    @Override
    @Operation(summary = "按 ID 查询房间")
    @PreAuthorize("hasAnyAuthority('game:*', 'game:play')")
    public R<GameRoomVO> getRoom(Long roomId) {
        GameRoomVO vo = GameRoomVO.builder()
                .id(roomId)
                .name("勇者房间 #" + roomId)
                .mode("5v5")
                .onlineCount(7)
                .owner("admin")
                .createTime(LocalDateTime.now())
                .build();
        return R.ok(vo);
    }
}
