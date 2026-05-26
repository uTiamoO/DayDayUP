package com.yuan.daydayup.game.api.feign;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.game.api.vo.GameRoomVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 游戏服务 Feign 客户端
 */
@FeignClient(name = "daydayup-game-biz", contextId = "gameClient", path = "/rooms")
public interface GameClient {

    /**
     * 按 ID 查询房间信息
     *
     * @param roomId 房间 ID
     * @return 房间视图
     */
    @GetMapping("/{roomId}")
    R<GameRoomVO> getRoom(@PathVariable("roomId") Long roomId);
}
