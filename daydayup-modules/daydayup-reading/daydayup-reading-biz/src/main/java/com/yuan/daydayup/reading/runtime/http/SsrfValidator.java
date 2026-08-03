package com.yuan.daydayup.reading.runtime.http;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * 出站 SSRF 防护（PRD §7）：
 *
 * <ol>
 *   <li>协议仅 http/https；</li>
 *   <li>目标 host 必须落在书源 {@code baseUrl} 同域（同 host，或同注册域的子域；baseUrl 为 IP 时须完全相同）；</li>
 *   <li>解析出的 IP 禁私网 / 回环 / 链路本地 / 组播 / 通配 / CGNAT / IPv6 ULA（可配置放行，仅联调用）。</li>
 * </ol>
 *
 * <p>命中拦截统一抛 {@link ErrorCode#READING_UPSTREAM_BLOCKED}（60103）。
 * 重定向的每一跳都必须重新调用 {@link #validate}；DNS 重绑定由 {@link HttpFetcher}
 * 的 OkHttp Dns 钩子二次校验兜底。</p>
 */
@Component
public class SsrfValidator {

    private final ReadingHttpProperties props;

    /** DNS 解析器，可注入替身以便离线单测 */
    private Resolver resolver = InetAddress::getAllByName;

    public SsrfValidator(ReadingHttpProperties props) {
        this.props = props;
    }

    @FunctionalInterface
    public interface Resolver {
        InetAddress[] resolve(String host) throws Exception;
    }

    public void setResolver(Resolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 校验一次出站目标（重定向的每一跳各调一次）。
     *
     * @param url     即将请求的绝对 URL
     * @param baseUrl 书源 baseUrl（同域白名单锚点）
     */
    public void validate(String url, String baseUrl) {
        validate(url, baseUrl, Set.of());
    }

    /** 校验一次出站目标，并在提供 allowlist 时要求 host 精确命中。 */
    public void validate(String url, String baseUrl, Set<String> allowedHosts) {
        URI uri = parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw blocked("非法协议: " + scheme);
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw blocked("目标缺少 host");
        }
        if (allowedHosts != null && !allowedHosts.isEmpty()
                && allowedHosts.stream().noneMatch(allowed -> host.equalsIgnoreCase(allowed))) {
            throw blocked("目标 host 不在精确允许范围内");
        }
        checkSameDomain(host, baseUrl);
        // 代理模式：目标经外部代理出站、不穿本地内网，本地 DNS 解析既不可靠（翻墙场景）也无意义，
        // 跳过「解析 + 私网 IP 段」校验；协议与同域校验已在上面完成，仍防书源规则跳到任意站点。
        if (props.getProxy() != null && props.getProxy().isEnabled()) {
            return;
        }
        checkResolved(host, resolve(host));
    }

    /** 同域校验：同 host / 同注册域子域；IP 字面量只允许完全相同 */
    void checkSameDomain(String host, String baseUrl) {
        String baseHost = parse(baseUrl).getHost();
        if (baseHost == null) {
            throw blocked("书源 baseUrl 非法: " + baseUrl);
        }
        host = host.toLowerCase();
        baseHost = baseHost.toLowerCase();
        if (host.equals(baseHost)) {
            return;
        }
        if (isIpLiteral(baseHost) || isIpLiteral(host)) {
            throw blocked("目标 " + host + " 与书源 " + baseHost + " 不同源");
        }
        String registrable = registrableSuffix(baseHost);
        if (host.equals(registrable) || host.endsWith("." + registrable)) {
            return;
        }
        throw blocked("目标 " + host + " 不在书源同域 " + registrable + " 内");
    }

    /** IP 段校验：私网 / 回环 / 链路本地 / 组播 / 通配 / CGNAT / IPv6 ULA 一律拦截 */
    public void checkResolved(String host, List<InetAddress> addresses) {
        if (props.isAllowPrivate()) {
            return;
        }
        if (addresses.isEmpty()) {
            throw blocked("目标域名无法解析: " + host);
        }
        for (InetAddress addr : addresses) {
            if (isForbidden(addr)) {
                throw blocked("目标解析到受限地址: " + host + " -> " + addr.getHostAddress());
            }
        }
    }

    public List<InetAddress> resolve(String host) {
        try {
            return List.of(resolver.resolve(host));
        } catch (Exception e) {
            throw blocked("目标域名解析失败: " + host);
        }
    }

    static boolean isForbidden(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()
                || addr.isAnyLocalAddress() || addr.isMulticastAddress()) {
            return true;
        }
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            // CGNAT 100.64.0.0/10
            return (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 0x40;
        }
        // IPv6 ULA fc00::/7
        return b.length == 16 && (b[0] & 0xFE) == (byte) 0xFC;
    }

    private static boolean isIpLiteral(String host) {
        return host.matches("\\d{1,3}(\\.\\d{1,3}){3}") || host.contains(":");
    }

    /** 取注册域近似值（末两级标签）。v1 不引 PSL 表，co.uk 类多级后缀按保守放宽处理 */
    private static String registrableSuffix(String host) {
        String[] labels = host.split("\\.");
        int n = labels.length;
        return n <= 2 ? host : labels[n - 2] + "." + labels[n - 1];
    }

    private static URI parse(String url) {
        try {
            return URI.create(url);
        } catch (Exception e) {
            throw blocked("URL 非法");
        }
    }

    private static BizException blocked(String detail) {
        return new BizException(ErrorCode.READING_UPSTREAM_BLOCKED, "出站被拦截：" + detail);
    }
}
