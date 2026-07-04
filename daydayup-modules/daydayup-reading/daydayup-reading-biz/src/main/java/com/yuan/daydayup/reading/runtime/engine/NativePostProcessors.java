package com.yuan.daydayup.reading.runtime.engine;

import com.yuan.daydayup.reading.compiler.model.RuleStep;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * 原生后处理执行（对应编译期由 {@link com.yuan.daydayup.reading.compiler.parser.NativeConverter}
 * 收敛出的 POST_PROCESSOR 步骤，rulemodel §5.1）：aes 解密 / base64 / hex / md5。
 *
 * <p>这些逻辑在编译期已从 {@code @js:} 桥调用识别为原生操作，运行时直接用 JDK 实现，不进 GraalJS。</p>
 */
public final class NativePostProcessors {

    private NativePostProcessors() {
    }

    public static String apply(RuleStep step, String value) {
        if (value == null) {
            return null;
        }
        String name = step.getPostProcessor();
        Map<String, String> params = step.getParams() == null ? Map.of() : step.getParams();
        return switch (name) {
            case "aesDecrypt" -> aesDecrypt(value, params);
            case "base64Decode" -> new String(Base64.getDecoder().decode(value.trim()), StandardCharsets.UTF_8);
            case "hexDecode" -> hexDecode(value.trim());
            case "md5" -> md5(value);
            default -> throw new IllegalArgumentException("未知原生后处理: " + name);
        };
    }

    private static String aesDecrypt(String value, Map<String, String> params) {
        try {
            String transformation = params.getOrDefault("transformation", "AES/CBC/PKCS5Padding");
            byte[] key = params.getOrDefault("key", "").getBytes(StandardCharsets.UTF_8);
            byte[] iv = params.getOrDefault("iv", "").getBytes(StandardCharsets.UTF_8);
            byte[] input = "base64".equals(params.get("input"))
                    ? Base64.getDecoder().decode(value.trim())
                    : value.getBytes(StandardCharsets.UTF_8);

            Cipher cipher = Cipher.getInstance(transformation);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            if (transformation.contains("CBC")) {
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
            } else {
                cipher.init(Cipher.DECRYPT_MODE, keySpec);
            }
            return new String(cipher.doFinal(input), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("aesDecrypt 失败: " + e.getMessage(), e);
        }
    }

    private static String hexDecode(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len - 1; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return new String(out, StandardCharsets.UTF_8);
    }

    private static String md5(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("md5 失败: " + e.getMessage(), e);
        }
    }
}
