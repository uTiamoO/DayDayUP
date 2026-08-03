package com.yuan.daydayup.reading.runtime.http;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.reading.observability.ReadingMetrics;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 解析运行时唯一的 HTTP 出站出口。
 *
 * <p>不变式：所有源站请求（含聚合、后台任务、后续 JS 桥 {@code java.ajax}）都必须走本类，
 * 从而统一享受 SSRF 校验（每一跳）、per-source 限速、超时与错误码映射。</p>
 *
 * <p>重定向手动跟随（客户端关闭自动跟随），每跳先过 {@link SsrfValidator} 再出站；
 * DNS 解析走 OkHttp Dns 钩子二次过滤，封死校验-请求间的 DNS 重绑定窗口。</p>
 */
@Slf4j
@Component
public class HttpFetcher {

    private final ReadingHttpProperties props;
    private final SsrfValidator ssrf;
    private final SourceRateLimiter rateLimiter;
    private final ReadingMetrics metrics;
    private final OkHttpClient client;

    public HttpFetcher(ReadingHttpProperties props, SsrfValidator ssrf, SourceRateLimiter rateLimiter,
                       ReadingMetrics metrics) {
        this.props = props;
        this.ssrf = ssrf;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
                .readTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .followRedirects(false)
                .followSslRedirects(false);

        ReadingHttpProperties.Proxy proxyCfg = props.getProxy();
        if (proxyCfg != null && proxyCfg.isEnabled()) {
            // 走外部代理出站（翻墙）：由代理负责连通与（SOCKS）远程 DNS，本地 Dns 钩子不再介入，
            // 避免本地被污染/解不出被墙域名时误拦。SSRF 的私网 IP 段校验在代理模式下由 SsrfValidator 跳过。
            java.net.Proxy.Type type = proxyCfg.getType() == ReadingHttpProperties.Proxy.Type.SOCKS
                    ? java.net.Proxy.Type.SOCKS
                    : java.net.Proxy.Type.HTTP;
            builder.proxy(new java.net.Proxy(type,
                    new java.net.InetSocketAddress(proxyCfg.getHost(), proxyCfg.getPort())));
        } else {
            // 直连：保留 OkHttp Dns 钩子二次过滤，封死校验-请求间的 DNS 重绑定窗口。
            builder.dns(hostname -> {
                var addresses = ssrf.resolve(hostname);
                ssrf.checkResolved(hostname, addresses);
                return addresses;
            });
        }
        this.client = builder.build();
    }

    /** 一次出站抓取请求 */
    @Value
    @Builder
    public static class FetchRequest {
        /** 限速键（书源 bookSourceUrl / identity.key） */
        String sourceKey;
        /** 书源 baseUrl，SSRF 同域锚点 */
        String baseUrl;
        /** 精确 host allowlist；空集合时沿用 baseUrl 同注册域策略 */
        Set<String> allowedHosts;
        /** 绝对 URL */
        String url;
        @Builder.Default
        String method = "GET";
        /** POST 体（已渲染） */
        String body;
        /** 显式字符集（书源声明），空则从 Content-Type 推断，再缺省 UTF-8 */
        String charset;
        /** 合并后的请求头（书源级 + 动作级） */
        Map<String, String> headers;
        /** Legado concurrentRate 原文 */
        String concurrentRate;
    }

    /** 抓取并解码为文本。失败映射 60101/60102/60103。 */
    public String fetch(FetchRequest req) {
        // 观测性旁路：整段抓取计时，出口按结果分类记 reading.fetch.duration（不改变任何抓取语义）
        long startNanos = System.nanoTime();
        String fetchResult = "failed";
        try {
            String result = doFetch(req);
            fetchResult = "success";
            return result;
        } catch (BizException e) {
            fetchResult = fetchResultOf(e);
            throw e;
        } finally {
            metrics.recordFetch(req.getSourceKey(), fetchResult, System.nanoTime() - startNanos);
        }
    }

    private String doFetch(FetchRequest req) {
        String url = req.getUrl();
        for (int hop = 0; hop <= props.getMaxRedirects(); hop++) {
            ssrf.validate(url, req.getBaseUrl(), req.getAllowedHosts());
            rateLimiter.acquire(req.getSourceKey(), req.getConcurrentRate());
            try (Response response = client.newCall(buildRequest(req, url)).execute()) {
                if (response.isRedirect()) {
                    String location = response.header("Location");
                    if (location == null || location.isBlank()) {
                        throw new BizException(ErrorCode.READING_UPSTREAM_FETCH_FAILED,
                                "源站重定向缺少 Location");
                    }
                    url = URI.create(url).resolve(location.strip()).toString();
                    continue;
                }
                if (!response.isSuccessful()) {
                    throw new BizException(ErrorCode.READING_UPSTREAM_FETCH_FAILED,
                            "源站返回 HTTP " + response.code());
                }
                return decode(response.body(), req.getCharset());
            } catch (SocketTimeoutException e) {
                throw new BizException(ErrorCode.READING_UPSTREAM_TIMEOUT, "源站抓取超时");
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                throw new BizException(ErrorCode.READING_UPSTREAM_FETCH_FAILED,
                        "源站抓取失败");
            } finally {
                rateLimiter.release(req.getSourceKey());
            }
        }
        throw new BizException(ErrorCode.READING_UPSTREAM_FETCH_FAILED,
                "重定向超过 " + props.getMaxRedirects() + " 跳");
    }

    /** 将抓取异常错误码归类为有界的 result tag：blocked(60103) / timeout(60102) / failed(其余)。 */
    private static String fetchResultOf(BizException e) {
        if (e.getCode() == ErrorCode.READING_UPSTREAM_BLOCKED.getCode()) {
            return "blocked";
        }
        if (e.getCode() == ErrorCode.READING_UPSTREAM_TIMEOUT.getCode()) {
            return "timeout";
        }
        return "failed";
    }

    private Request buildRequest(FetchRequest req, String url) {
        Request.Builder builder = new Request.Builder().url(url);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", props.getUserAgent());
        if (req.getHeaders() != null) {
            headers.putAll(req.getHeaders());
        }
        if (!sameHost(req.getUrl(), url)) {
            headers.entrySet().removeIf(entry -> isSensitiveHeader(entry.getKey()));
        }
        headers.forEach(builder::header);

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            Charset cs = charsetOf(req.getCharset());
            String contentType = headers.getOrDefault("Content-Type",
                    "application/x-www-form-urlencoded; charset=" + cs.name());
            builder.post(RequestBody.create(
                    req.getBody() == null ? new byte[0] : req.getBody().getBytes(cs),
                    MediaType.parse(contentType)));
        } else {
            builder.get();
        }
        return builder.build();
    }

    private static String decode(ResponseBody body, String explicitCharset) throws Exception {
        if (body == null) {
            return "";
        }
        byte[] bytes = body.bytes();
        if (explicitCharset != null && !explicitCharset.isBlank()) {
            return new String(bytes, charsetOf(explicitCharset));
        }
        MediaType mediaType = body.contentType();
        Charset cs = mediaType != null ? mediaType.charset(StandardCharsets.UTF_8) : StandardCharsets.UTF_8;
        return new String(bytes, cs == null ? StandardCharsets.UTF_8 : cs);
    }

    private static Charset charsetOf(String name) {
        try {
            return name == null || name.isBlank() ? StandardCharsets.UTF_8 : Charset.forName(name);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static boolean sameHost(String first, String second) {
        try {
            String firstHost = URI.create(first).getHost();
            String secondHost = URI.create(second).getHost();
            return firstHost != null && secondHost != null && firstHost.equalsIgnoreCase(secondHost);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isSensitiveHeader(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase().replace("-", "").replace("_", "");
        return normalized.contains("authorization")
                || normalized.contains("cookie")
                || normalized.contains("token")
                || normalized.contains("device");
    }
}
