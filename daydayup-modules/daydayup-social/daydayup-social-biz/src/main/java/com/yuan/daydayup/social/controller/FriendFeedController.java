package com.yuan.daydayup.social.controller;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.game.api.feign.GameClient;
import com.yuan.daydayup.game.api.vo.GameRoomVO;
import com.yuan.daydayup.social.api.feign.SocialClient;
import com.yuan.daydayup.social.api.vo.FriendFeedVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 好友动态接口（P2 阶段 mock 数据 + Feign 互调示例）
 *
 * <p>当请求到达时，本接口会通过 {@link GameClient} 调用 {@code daydayup-game-biz}
 * 拉取关联房间信息，把"游戏 + 社交"聚合后返回——验证 Feign 服务间互调链路。</p>
 */
@Slf4j
@RestController
@Tag(name = "好友动态")
public class FriendFeedController implements SocialClient {

    private final GameClient gameClient;

    public FriendFeedController(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    @Override
    @Operation(summary = "拉取最近的好友动态（含关联游戏房间）")
    @PreAuthorize("hasAnyAuthority('social:*', 'social:read')")
    public R<List<FriendFeedVO>> recent() {
        R<GameRoomVO> roomResult = gameClient.getRoom(1001L);
        String roomName = roomResult.isSuccess() && roomResult.getData() != null
                ? roomResult.getData().getName()
                : "未知房间";

        FriendFeedVO feed = FriendFeedVO.builder()
                .id(1L)
                .publisher("admin")
                .content("刚和兄弟们开黑赢了一把！")
                .relatedRoomName(roomName)
                .publishTime(LocalDateTime.now())
                .build();

        return R.ok(List.of(feed));
    }
}
