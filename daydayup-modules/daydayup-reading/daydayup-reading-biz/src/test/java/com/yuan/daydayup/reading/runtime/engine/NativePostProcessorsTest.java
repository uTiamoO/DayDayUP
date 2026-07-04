package com.yuan.daydayup.reading.runtime.engine;

import com.yuan.daydayup.reading.compiler.model.RuleStep;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link NativePostProcessors} 单测：验证原生后处理执行（aes/base64/hex/md5）。
 */
class NativePostProcessorsTest {

    static final String KEY = "f041c49714d39908";
    static final String IV = "0123456789abcdef";
    static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    /** 测试用：与运行时 aesDecrypt 对称的加密，产出 base64 密文 */
    static String aesEncryptBase64(String plain) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void aesDecrypt_roundtrip() throws Exception {
        String plain = "http://api.jmlldsc.com/697/697647/91038.json";
        String cipherB64 = aesEncryptBase64(plain);

        RuleStep step = RuleStep.postProcessor("aesDecrypt", Map.of(
                "key", KEY, "iv", IV, "transformation", TRANSFORMATION, "input", "base64"));
        assertEquals(plain, NativePostProcessors.apply(step, cipherB64));
    }

    @Test
    void base64Decode() {
        String enc = Base64.getEncoder().encodeToString("你好abc".getBytes(StandardCharsets.UTF_8));
        RuleStep step = RuleStep.postProcessor("base64Decode", Map.of());
        assertEquals("你好abc", NativePostProcessors.apply(step, enc));
    }

    @Test
    void hexDecode() {
        RuleStep step = RuleStep.postProcessor("hexDecode", Map.of());
        // "abc" => 616263
        assertEquals("abc", NativePostProcessors.apply(step, "616263"));
    }

    @Test
    void md5() {
        RuleStep step = RuleStep.postProcessor("md5", Map.of());
        assertEquals("900150983cd24fb0d6963f7d28e17f72", NativePostProcessors.apply(step, "abc"));
    }
}
