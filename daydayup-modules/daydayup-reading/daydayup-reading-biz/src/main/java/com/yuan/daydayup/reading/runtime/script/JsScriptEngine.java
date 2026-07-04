package com.yuan.daydayup.reading.runtime.script;

import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 受控 GraalJS 脚本执行器（engine-decision 规格 §4）。
 *
 * <p>安全边界：</p>
 * <ul>
 *   <li><b>沙箱</b>：默认 {@code Context.newBuilder("js").build()} —— 主机类访问 / IO / 线程
 *       创建全部关闭，{@code Java.type} 不可用；宿主能力仅经显式注入的 {@link JsBridge} allowlist 暴露；</li>
 *   <li><b>超时</b>：看门狗线程在 {@code timeoutMs} 后 {@link Context#interrupt} 强制中断
 *       （切片 0 实测 Rhino 同场景中断失败，GraalJS 可靠），死循环不钉住工作线程；</li>
 *   <li><b>语句数上限</b>：{@link ResourceLimits} 第二道保险。</li>
 * </ul>
 *
 * <p>共享 {@link Engine}（摊薄编译/预热），每次执行独立 {@link Context}（无跨脚本状态泄漏）。</p>
 */
@Slf4j
@Component
public class JsScriptEngine implements AutoCloseable {

    private final ReadingScriptProperties props;
    private final Engine engine;
    private final ScheduledExecutorService watchdog;

    public JsScriptEngine(ReadingScriptProperties props) {
        this.props = props;
        this.engine = Engine.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        this.watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reading-js-watchdog");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 执行一段书源脚本（Legado {@code @js:} 体），脚本最后一个表达式的值即结果。
     *
     * @param scriptBody 脚本体
     * @param bindings   注入的全局绑定（result / baseUrl / key / page …）
     * @param javaBridge {@code java} 桥对象，可为 null（纯计算脚本）
     */
    public String eval(String scriptBody, Map<String, Object> bindings, ProxyObject javaBridge) {
        try (Context ctx = Context.newBuilder("js")
                .engine(engine)
                .resourceLimits(ResourceLimits.newBuilder()
                        .statementLimit(props.getStatementLimit(), null)
                        .build())
                .build()) {

            Value globals = ctx.getBindings("js");
            if (bindings != null) {
                bindings.forEach(globals::putMember);
            }
            if (javaBridge != null) {
                globals.putMember("java", javaBridge);
            }

            ScheduledFuture<?> kill = watchdog.schedule(() -> {
                try {
                    ctx.interrupt(Duration.ofSeconds(5));
                } catch (Exception e) {
                    log.warn("[js-engine] 看门狗中断失败，强制 cancel", e);
                    ctx.close(true);
                }
            }, props.getTimeoutMs(), TimeUnit.MILLISECONDS);
            try {
                return toStringValue(ctx.eval("js", scriptBody));
            } finally {
                kill.cancel(false);
            }
        } catch (PolyglotException e) {
            throw translate(e, scriptBody);
        }
    }

    private BizException translate(PolyglotException e, String scriptBody) {
        if (e.isInterrupted() || e.isCancelled()) {
            return new BizException(ErrorCode.READING_RULE_RUNTIME_FAILED,
                    "脚本执行超时（>" + props.getTimeoutMs() + "ms）已被看门狗中断");
        }
        // 桥内抛出的 BizException（如 SSRF 60103）原样上抛，不吞语义
        if (e.isHostException() && e.asHostException() instanceof BizException biz) {
            return biz;
        }
        return new BizException(ErrorCode.READING_RULE_RUNTIME_FAILED,
                "脚本执行失败: " + e.getMessage());
    }

    private static String toStringValue(Value v) {
        if (v == null || v.isNull()) {
            return null;
        }
        return v.isString() ? v.asString() : v.toString();
    }

    @PreDestroy
    @Override
    public void close() {
        watchdog.shutdownNow();
        engine.close();
    }
}
