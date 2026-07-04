package com.yuan.daydayup.reading.runtime.script;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 受控 JS 脚本执行配置（{@code reading.script.*}）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "reading.script")
public class ReadingScriptProperties {

    /** 单脚本执行超时（毫秒），超时由看门狗 Context.interrupt 中断 */
    private long timeoutMs = 3000;

    /** 语句数上限（ResourceLimits，第二道保险） */
    private long statementLimit = 2_000_000;
}
