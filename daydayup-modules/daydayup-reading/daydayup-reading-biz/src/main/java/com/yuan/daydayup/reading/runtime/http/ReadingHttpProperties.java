package com.yuan.daydayup.reading.runtime.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 解析运行时 HTTP 出站配置（{@code reading.http.*}）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "reading.http")
public class ReadingHttpProperties {

    /** 连接超时（毫秒） */
    private long connectTimeoutMs = 5000;

    /** 读超时（毫秒） */
    private long readTimeoutMs = 10000;

    /** 最大重定向跳数（每跳重新过 SSRF 校验） */
    private int maxRedirects = 5;

    /**
     * 是否放行私网/回环目标。生产必须 false；仅本机联调 / 单测（MockWebServer 跑在 127.0.0.1）置 true。
     */
    private boolean allowPrivate = false;

    /** 默认 UA（书源自带 User-Agent 时被覆盖） */
    private String userAgent = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";

    /** 书源未声明 concurrentRate 时的默认最小请求间隔（毫秒） */
    private long defaultMinIntervalMs = 200;

    /** 单书源并发上限 */
    private int maxConcurrentPerSource = 2;

    /** 限速等待上限（毫秒），超过视为源站过载 */
    private long rateWaitTimeoutMs = 10000;

    /** 出站代理（翻墙/科学上网）。默认关闭；需要访问被墙/被 Cloudflare 拦的源站时开启。 */
    private Proxy proxy = new Proxy();

    /**
     * 出站代理配置。
     *
     * <p>开启后所有源站请求经代理出站；且因目标经外部代理到达、不穿本地内网，
     * SSRF 的「本地 DNS 解析 + 私网 IP 段」校验会跳过（本地 DNS 在翻墙场景不可靠且无意义），
     * 但协议与书源同域校验仍然生效。</p>
     */
    @Data
    public static class Proxy {
        /** 是否启用出站代理 */
        private boolean enabled = false;
        /** 代理类型：HTTP 或 SOCKS */
        private Type type = Type.HTTP;
        /** 代理主机（如本机 Clash/V2Ray：127.0.0.1） */
        private String host = "127.0.0.1";
        /** 代理端口（Clash 常见 7890；V2rayN HTTP 常见 10809 / SOCKS 10808） */
        private int port = 7890;

        public enum Type {
            HTTP, SOCKS
        }
    }
}
