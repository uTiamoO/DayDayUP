package com.yuan.daydayup.game.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 游戏房间视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "游戏房间")
public class GameRoomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "房间 ID", example = "1001")
    private Long id;

    @Schema(description = "房间名称")
    private String name;

    @Schema(description = "游戏模式：1v1 / 3v3 / 5v5")
    private String mode;

    @Schema(description = "当前在线玩家数")
    private Integer onlineCount;

    @Schema(description = "房主用户名")
    private String owner;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
