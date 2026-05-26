package com.yuan.daydayup.social.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友动态视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友动态")
public class FriendFeedVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "动态 ID")
    private Long id;

    @Schema(description = "发布者用户名")
    private String publisher;

    @Schema(description = "动态正文")
    private String content;

    @Schema(description = "关联的游戏房间名（聚合自 game 服务）")
    private String relatedRoomName;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
