package com.yuan.daydayup.reading.source.nativeparser.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 原生书源的一次出站抓取计划。
 *
 * <p>native parser 负责把「站点动作 + 入参」翻译成一个可执行的抓取请求（URL / method /
 * body / charset / headers），交由统一 {@code HttpFetcher} 出站（SSRF + 限速 + 超时）。
 * parser 不自己创建 HTTP 客户端，出站语义与 legacy RuleModel 路径保持一致。</p>
 */
public final class NativeFetchPlan {

    private final String url;
    private final String method;
    private final String body;
    private final String charset;
    private final Map<String, String> headers;

    private NativeFetchPlan(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.body = builder.body;
        this.charset = builder.charset;
        this.headers = builder.headers;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** GET 抓取的便捷构造 */
    public static NativeFetchPlan get(String url) {
        return builder().url(url).build();
    }

    public String url() {
        return url;
    }

    public String method() {
        return method;
    }

    public String body() {
        return body;
    }

    public String charset() {
        return charset;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public static final class Builder {
        private String url;
        private String method = "GET";
        private String body;
        private String charset;
        private Map<String, String> headers = new LinkedHashMap<>();

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder charset(String charset) {
            this.charset = charset;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }

        public NativeFetchPlan build() {
            return new NativeFetchPlan(this);
        }
    }
}
