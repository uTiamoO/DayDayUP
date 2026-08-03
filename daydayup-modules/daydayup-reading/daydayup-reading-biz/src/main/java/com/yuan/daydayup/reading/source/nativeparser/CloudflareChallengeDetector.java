package com.yuan.daydayup.reading.source.nativeparser;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Cloudflare / Turnstile 人机验证挑战检测器。
 *
 * <p>用于在跑业务 selector 之前短路判断：若响应体是挑战页，则归一化到
 * {@code verification_required} 诊断，而不是把挑战 HTML 当成普通 parse_error。
 * 本类只做检测，绝不尝试自动绕过、复用 cookie 或注入验证态（合规约束，见 S2A spec）。</p>
 *
 * <p>标志来自 69shuba 站点研究实测：HTTP 403 + {@code server: cloudflare} +
 * {@code Just a moment...} + {@code challenges.cloudflare.com} + {@code challenge-platform}
 * + {@code turnstile} + {@code cf-ray}。</p>
 */
public final class CloudflareChallengeDetector {

    /** 挑战页正文标志（大小写不敏感） */
    private static final List<Pattern> BODY_MARKERS = List.of(
            Pattern.compile("just a moment", Pattern.CASE_INSENSITIVE),
            Pattern.compile("challenges\\.cloudflare\\.com", Pattern.CASE_INSENSITIVE),
            Pattern.compile("challenge-platform", Pattern.CASE_INSENSITIVE),
            Pattern.compile("cf-browser-verification", Pattern.CASE_INSENSITIVE),
            Pattern.compile("turnstile", Pattern.CASE_INSENSITIVE),
            Pattern.compile("_cf_chl_opt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("验证您是真人"),
            Pattern.compile("verify\\.php", Pattern.CASE_INSENSITIVE));

    private CloudflareChallengeDetector() {
    }

    /**
     * 判断 HTML 响应体是否为 Cloudflare / Turnstile 挑战页。
     *
     * @param html 已解码的响应体
     * @return true 表示命中挑战，应短路到 {@code verification_required}
     */
    public static boolean isChallenge(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        // 挑战页普遍短小且不含真正业务内容，这里只按标志判断，避免误伤正常正文里偶现的关键词。
        for (Pattern marker : BODY_MARKERS) {
            if (marker.matcher(html).find()) {
                return true;
            }
        }
        return false;
    }
}
