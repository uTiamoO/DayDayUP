package com.yuan.daydayup.reading.runtime.script;

import com.yuan.daydayup.reading.compiler.model.RuleStep;
import com.yuan.daydayup.reading.runtime.engine.NativePostProcessors;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 暴露给书源脚本的 {@code java} 桥对象（rulemodel §7.2 P0 子集 allowlist）。
 *
 * <ul>
 *   <li>{@code java.ajax(url)} / {@code java.get(url, headers)} —— HTTP 出站，<b>强制走
 *       {@link HttpGet} 回调（即 HttpFetcher 统一出口）</b>，同样吃 SSRF 校验与 per-source 限速，不可绕行；</li>
 *   <li>{@code java.put(key, value)} / {@code java.get(key)}（单参）—— 执行期变量表；</li>
 *   <li>crypto 族（aesBase64DecodeToString / base64Decode / hexDecodeToString / md5Encode）——
 *       委托 {@link NativePostProcessors}（多数已被编译期收敛，这里兜底脚本内动态调用）；</li>
 *   <li>UI 类（toast / log 等）—— no-op；</li>
 *   <li>allowlist 之外的成员返回 null，脚本侧调用即报错（fail-fast，不静默出错值）。</li>
 * </ul>
 */
@Slf4j
public class JsBridge implements ProxyObject {

    /** HTTP 出站回调（由 SourceReadingService 绑定到 HttpFetcher 统一出口） */
    @FunctionalInterface
    public interface HttpGet {
        String get(String url, Map<String, String> headers);
    }

    private static final Set<String> UI_NO_OPS = Set.of(
            "toast", "longToast", "log", "logType", "startBrowser", "startBrowserAwait");

    private static final List<String> MEMBERS = List.of(
            "ajax", "get", "put",
            "aesBase64DecodeToString", "base64Decode", "hexDecodeToString", "md5Encode",
            "toast", "longToast", "log", "logType", "startBrowser", "startBrowserAwait");

    private final Map<String, String> variables;
    private final HttpGet http;

    public JsBridge(Map<String, String> variables, HttpGet http) {
        this.variables = variables == null ? new LinkedHashMap<>() : variables;
        this.http = http;
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case "ajax" -> (ProxyExecutable) args -> httpGet(str(args, 0), Map.of());
            case "get" -> (ProxyExecutable) args -> args.length >= 2
                    ? httpGet(str(args, 0), toHeaderMap(args[1]))
                    : variables.get(str(args, 0));
            case "put" -> (ProxyExecutable) args -> {
                String v = str(args, 1);
                variables.put(str(args, 0), v);
                return v;
            };
            case "aesBase64DecodeToString" -> (ProxyExecutable) args ->
                    NativePostProcessors.apply(RuleStep.postProcessor("aesDecrypt", Map.of(
                            "key", str(args, 1),
                            "transformation", str(args, 2),
                            "iv", str(args, 3),
                            "input", "base64")), str(args, 0));
            case "base64Decode" -> (ProxyExecutable) args ->
                    NativePostProcessors.apply(RuleStep.postProcessor("base64Decode", Map.of()), str(args, 0));
            case "hexDecodeToString" -> (ProxyExecutable) args ->
                    NativePostProcessors.apply(RuleStep.postProcessor("hexDecode", Map.of()), str(args, 0));
            case "md5Encode" -> (ProxyExecutable) args ->
                    NativePostProcessors.apply(RuleStep.postProcessor("md5", Map.of()), str(args, 0));
            default -> UI_NO_OPS.contains(key) ? (ProxyExecutable) args -> {
                log.debug("[js-bridge] UI 桥 no-op: {}", key);
                return null;
            } : null;
        };
    }

    @Override
    public Object getMemberKeys() {
        return MEMBERS;
    }

    @Override
    public boolean hasMember(String key) {
        return MEMBERS.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("java 桥对象只读");
    }

    private String httpGet(String url, Map<String, String> headers) {
        if (http == null) {
            throw new IllegalStateException("本执行上下文未开放网络出站（java.ajax/get 不可用）");
        }
        return http.get(url, headers);
    }

    private static String str(Value[] args, int i) {
        if (i >= args.length || args[i] == null || args[i].isNull()) {
            return null;
        }
        return args[i].isString() ? args[i].asString() : args[i].toString();
    }

    private static Map<String, String> toHeaderMap(Value v) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (v != null && v.hasMembers()) {
            for (String k : v.getMemberKeys()) {
                Value member = v.getMember(k);
                if (member != null && !member.isNull()) {
                    headers.put(k, member.isString() ? member.asString() : member.toString());
                }
            }
        }
        return headers;
    }
}
