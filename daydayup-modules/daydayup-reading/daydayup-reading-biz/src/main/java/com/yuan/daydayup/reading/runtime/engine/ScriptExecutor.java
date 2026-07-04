package com.yuan.daydayup.reading.runtime.engine;

/**
 * ScriptStep 执行回调：由调用方（如 SourceReadingService）按请求上下文绑定
 * GraalJS 引擎 + java 桥后传入抽取器。离线执行（不传）时遇 ScriptStep 直接失败。
 */
@FunctionalInterface
public interface ScriptExecutor {

    /**
     * @param scriptBody    脚本体（Legado @js: 后的内容）
     * @param currentResult 链上当前值（注入脚本的 result 绑定）
     * @return 脚本产出的新值
     */
    String run(String scriptBody, String currentResult);
}
