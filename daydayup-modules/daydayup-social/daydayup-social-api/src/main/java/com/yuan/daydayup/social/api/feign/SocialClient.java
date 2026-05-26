package com.yuan.daydayup.social.api.feign;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.social.api.vo.FriendFeedVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 社交服务 Feign 客户端
 */
@FeignClient(name = "daydayup-social-biz", contextId = "socialClient", path = "/feeds")
public interface SocialClient {

    /**
     * 拉取最近的好友动态
     */
    @GetMapping("/recent")
    R<List<FriendFeedVO>> recent();
}
