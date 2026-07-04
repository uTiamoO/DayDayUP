package com.yuan.daydayup.reading.runtime.http;

import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SsrfValidator} 单测：同域白名单 + 私网 IP 拦截（离线，DNS 用替身）。
 */
class SsrfValidatorTest {

    private static final int BLOCKED = 60103;

    private SsrfValidator validator(String... hostToIp) {
        SsrfValidator v = new SsrfValidator(new ReadingHttpProperties());
        v.setResolver(host -> {
            for (int i = 0; i + 1 < hostToIp.length; i += 2) {
                if (hostToIp[i].equals(host)) {
                    return new InetAddress[]{ip(hostToIp[i + 1])};
                }
            }
            throw new java.net.UnknownHostException(host);
        });
        return v;
    }

    private static InetAddress ip(String dotted) {
        try {
            String[] p = dotted.split("\\.");
            byte[] b = new byte[4];
            for (int i = 0; i < 4; i++) {
                b[i] = (byte) Integer.parseInt(p[i]);
            }
            return InetAddress.getByAddress(dotted, b);
        } catch (Exception e) {
            throw new IllegalArgumentException(dotted, e);
        }
    }

    @Test
    void crossDomainBlocked() {
        BizException e = assertThrows(BizException.class, () ->
                validator().validate("http://evil.com/steal", "https://m.libahao.com"));
        assertEquals(BLOCKED, e.getCode());
    }

    @Test
    void subdomainOfSameRegistrableDomainAllowed() {
        assertDoesNotThrow(() -> validator("api.libahao.com", "93.184.216.34")
                .validate("https://api.libahao.com/x", "https://m.libahao.com"));
    }

    @Test
    void privateIpBlocked() {
        BizException e = assertThrows(BizException.class, () ->
                validator("api.libahao.com", "192.168.1.10")
                        .validate("https://api.libahao.com/x", "https://m.libahao.com"));
        assertEquals(BLOCKED, e.getCode());
    }

    @Test
    void loopbackLiteralBlocked() {
        BizException e = assertThrows(BizException.class, () ->
                validator("127.0.0.1", "127.0.0.1")
                        .validate("http://127.0.0.1:8080/admin", "http://127.0.0.1:8080"));
        assertEquals(BLOCKED, e.getCode());
    }

    @Test
    void ipLiteralBaseRequiresExactHost() {
        BizException e = assertThrows(BizException.class, () ->
                validator().validate("http://10.0.0.2/x", "http://10.0.0.1"));
        assertEquals(BLOCKED, e.getCode());
    }

    @Test
    void nonHttpSchemeBlocked() {
        BizException e = assertThrows(BizException.class, () ->
                validator().validate("ftp://m.libahao.com/x", "https://m.libahao.com"));
        assertEquals(BLOCKED, e.getCode());
    }

    @Test
    void forbiddenRanges() {
        assertTrue(SsrfValidator.isForbidden(ip("10.1.2.3")));
        assertTrue(SsrfValidator.isForbidden(ip("172.16.0.1")));
        assertTrue(SsrfValidator.isForbidden(ip("169.254.1.1")));
        assertTrue(SsrfValidator.isForbidden(ip("100.64.0.1")));   // CGNAT
        assertTrue(SsrfValidator.isForbidden(ip("0.0.0.0")));
        assertFalse(SsrfValidator.isForbidden(ip("93.184.216.34")));
        assertFalse(SsrfValidator.isForbidden(ip("100.128.0.1"))); // CGNAT 段外
    }

    @Test
    void allowPrivateSkipsIpCheck() {
        ReadingHttpProperties props = new ReadingHttpProperties();
        props.setAllowPrivate(true);
        SsrfValidator v = new SsrfValidator(props);
        v.setResolver(host -> new InetAddress[]{ip("127.0.0.1")});
        assertDoesNotThrow(() -> v.validate("http://127.0.0.1:9999/x", "http://127.0.0.1:9999"));
    }
}
