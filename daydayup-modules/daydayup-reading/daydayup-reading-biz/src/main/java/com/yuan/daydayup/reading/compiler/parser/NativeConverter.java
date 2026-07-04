package com.yuan.daydayup.reading.compiler.parser;

import com.yuan.daydayup.reading.compiler.model.RuleStep;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 原生收敛（rulemodel §5.1）：把「参数全字面量」的已知桥函数脚本模式匹配为原生
 * {@link RuleStep} POST_PROCESSOR，不进 JS 引擎，从而把这类书源从 degraded 提升为 full。
 *
 * <p>仅收敛 crypto/编码族；任意逻辑脚本仍落 SCRIPT 由运行时 GraalJS 兜底。</p>
 */
public final class NativeConverter {

    private NativeConverter() {
    }

    // java.aesBase64DecodeToString(result,"KEY","AES/CBC/PKCS5Padding","IV")
    private static final Pattern AES_BASE64 = Pattern.compile(
            "java\\.aesBase64DecodeToString\\s*\\(\\s*result\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*,\\s*\"([^\"]*)\"\\s*\\)");
    // java.base64Decode(result) / java.base64DecodeToString(result)
    private static final Pattern BASE64 = Pattern.compile(
            "java\\.base64Decode(?:ToString)?\\s*\\(\\s*result\\s*\\)");
    private static final Pattern HEX = Pattern.compile(
            "java\\.hexDecodeToString\\s*\\(\\s*result\\s*\\)");
    private static final Pattern MD5 = Pattern.compile(
            "java\\.md5Encode\\s*\\(\\s*result\\s*\\)");

    /**
     * 尝试将脚本片段收敛为原生后处理步骤。
     *
     * @return 命中则返回 POST_PROCESSOR 步骤，否则 {@link Optional#empty()}（应保留为 SCRIPT）
     */
    public static Optional<RuleStep> tryConvert(String script) {
        if (script == null || script.isBlank()) {
            return Optional.empty();
        }
        String s = script.trim();

        Matcher aes = AES_BASE64.matcher(s);
        if (aes.find()) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("key", aes.group(1));
            params.put("transformation", aes.group(2));
            params.put("iv", aes.group(3));
            params.put("input", "base64");
            return Optional.of(RuleStep.postProcessor("aesDecrypt", params));
        }
        if (BASE64.matcher(s).find()) {
            return Optional.of(RuleStep.postProcessor("base64Decode", Map.of()));
        }
        if (HEX.matcher(s).find()) {
            return Optional.of(RuleStep.postProcessor("hexDecode", Map.of()));
        }
        if (MD5.matcher(s).find()) {
            return Optional.of(RuleStep.postProcessor("md5", Map.of()));
        }
        return Optional.empty();
    }
}
