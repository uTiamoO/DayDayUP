package com.yuan.daydayup.reading.runtime.script;

import com.yuan.daydayup.common.core.exception.BizException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JsScriptEngine} + {@link JsBridge} 单测：沙箱 / 看门狗超时中断 / 桥 allowlist。
 */
class JsScriptEngineTest {

    private static JsScriptEngine engine;

    @BeforeAll
    static void setUp() {
        ReadingScriptProperties props = new ReadingScriptProperties();
        props.setTimeoutMs(1500);
        engine = new JsScriptEngine(props);
    }

    @AfterAll
    static void tearDown() {
        engine.close();
    }

    @Test
    void evalExpressionWithResultBinding() {
        String out = engine.eval("result.toUpperCase() + '_x'", Map.of("result", "abc"), null);
        assertEquals("ABC_x", out);
    }

    @Test
    void infiniteLoopInterruptedByWatchdog() {
        long start = System.currentTimeMillis();
        BizException e = assertThrows(BizException.class,
                () -> engine.eval("var i=0; while(true){ i++; }", Map.of(), null));
        long elapsed = System.currentTimeMillis() - start;
        assertEquals(60202, e.getCode());
        // AC：死循环被中断、不钉住线程（1.5s 超时 + 中断余量内返回）
        assertTrue(elapsed < 8000, "看门狗未及时中断，elapsed=" + elapsed);
    }

    @Test
    void sandboxDeniesHostClassAccess() {
        String out = engine.eval("""
                (function(){ try { var S = Java.type('java.lang.System'); return S.getProperty('user.home'); }
                             catch (e) { return 'BLOCKED'; } })()
                """, Map.of(), null);
        assertEquals("BLOCKED", out);
    }

    @Test
    void bridgePutGetVariables() {
        JsBridge bridge = new JsBridge(new HashMap<>(), null);
        String out = engine.eval("java.put('a', result); java.get('a') + '_v'",
                Map.of("result", "hello"), bridge);
        assertEquals("hello_v", out);
    }

    @Test
    void bridgeAesDecode() throws Exception {
        String plain = "http://api.jmlldsc.com/697/697647/91038.json";
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec("f041c49714d39908".getBytes(StandardCharsets.UTF_8), "AES"),
                new IvParameterSpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        String cipherB64 = Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));

        JsBridge bridge = new JsBridge(new HashMap<>(), null);
        String out = engine.eval(
                "java.aesBase64DecodeToString(result,'f041c49714d39908','AES/CBC/PKCS5Padding','0123456789abcdef')",
                Map.of("result", cipherB64), bridge);
        assertEquals(plain, out);
    }

    @Test
    void bridgeAjaxGoesThroughCallback() {
        JsBridge bridge = new JsBridge(new HashMap<>(),
                (url, headers) -> "resp-of:" + url + ";ua=" + headers.getOrDefault("UA", "-"));
        assertEquals("resp-of:http://h/x;ua=-",
                engine.eval("java.ajax('http://h/x')", Map.of(), bridge));
        assertEquals("resp-of:http://h/y;ua=ok",
                engine.eval("java.get('http://h/y', {'UA':'ok'})", Map.of(), bridge));
    }

    @Test
    void uiBridgeIsNoOp() {
        JsBridge bridge = new JsBridge(new HashMap<>(), null);
        assertEquals("done", engine.eval("java.toast('hi'); java.log('x'); 'done'", Map.of(), bridge));
    }

    @Test
    void unknownBridgeMemberFailsFast() {
        JsBridge bridge = new JsBridge(new HashMap<>(), null);
        BizException e = assertThrows(BizException.class,
                () -> engine.eval("java.readFile('/etc/passwd')", Map.of(), bridge));
        assertEquals(60202, e.getCode());
    }

    @Test
    void nullResultReturnsNull() {
        assertNull(engine.eval("null", Map.of(), null));
    }
}
