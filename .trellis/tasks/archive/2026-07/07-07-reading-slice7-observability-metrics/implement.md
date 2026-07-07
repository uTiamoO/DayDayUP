# Implementation Plan: 阅读中台切片7：业务观测性埋点

## Pre-check

- 当前任务：`.trellis/tasks/07-07-reading-slice7-observability-metrics`。
- 阅读顺序：`prd.md` → `design.md` → 本文件。
- Maven 使用 JDK 21：`JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9`。
- 关键前提（已核实）：`MeterRegistry` 已由 actuator 传递可用；`micrometer-registry-prometheus` 仅在根 pom `dependencyManagement`，需在 reading-biz 实际引入。

## Steps

1. **引入 Prometheus registry 依赖**
   - `daydayup-reading-biz/pom.xml` 增加 `io.micrometer:micrometer-registry-prometheus`，不写版本号（走根 pom 管理）。
   - 运行 `mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am dependency:resolve` 或直接 test-compile 验证解析。

2. **新增 `ReadingMetrics` 薄封装组件**
   - 位置：`com.yuan.daydayup.reading.observability.ReadingMetrics`。
   - 构造注入 `MeterRegistry`。
   - 方法：`recordFetch(String sourceKey, String result, long durationNanos)`、`recordCompile(String grade)`、`recordSanitize(String result, Integer qualityScore)`、`recordTask(String type, String result)`。
   - 每个方法内部 try/catch，异常仅 `log.debug`，不外抛。
   - metric 名与 tag 按 design 指标清单实现；`source` tag 收敛在单一私有方法组装，便于后续降级。

3. **埋点 `HttpFetcher.fetch()`**
   - 方法入口记 `long start = System.nanoTime()`。
   - 用局部 `String result` 标记，正常 return 前置 `success`；`SocketTimeoutException` 分支置 `timeout`；`BizException` 分支按 `ErrorCode`（`READING_UPSTREAM_BLOCKED` → `blocked`，其余 → `failed`）；通用 Exception → `failed`。
   - 在 finally 或各出口统一调 `metrics.recordFetch(req.getSourceKey(), result, System.nanoTime()-start)`。
   - 严格保证：SSRF 校验、限速 acquire/release、重定向、错误码映射逻辑一字不改。

4. **埋点 `RuleCompileServiceImpl`**
   - `doCompile` 成功得到 `grade` 后调 `metrics.recordCompile(grade)`。
   - `compileAllEnabled` catch 分支调 `metrics.recordCompile("failed")`。
   - 构造注入 `ReadingMetrics`。

5. **埋点 `ContentSanitizeServiceImpl.sanitize()`**
   - pipeline 异常 archive failed 后、抛异常前调 `metrics.recordSanitize("failed", null)`。
   - 正常路径按 `result.getRunStatus()` 调 `metrics.recordSanitize(status, result.getQualityScore())`。
   - 构造注入 `ReadingMetrics`。

6. **埋点 `ReadingTaskWorkerImpl.drain()`**
   - succeeded/partial/retry/failed 四个分支各调 `metrics.recordTask(task.getTaskType(), result)`。
   - 构造注入 `ReadingMetrics`。

7. **测试**
   - `ReadingMetricsTest`：`SimpleMeterRegistry` 注入，断言各 record 写入预期 meter 名 + tag；断言异常入参静默不抛。
   - 扩充 `HttpFetcherTest`：mockwebserver 成功/超时/HTTP 错误分别断言 `reading.fetch.duration` 的 `result` tag；SSRF 拦截断言 `result=blocked`。
   - 扩充 `RuleCompileServiceTest` / `ContentSanitizeServiceTest` / `ReadingTaskWorkerTest`：注入 `SimpleMeterRegistry`，断言对应计数。
   - 运行：
     ```bash
     JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test
     ```

8. **补 spec 与进度文档**
   - 新增 `.trellis/spec/backend/reading-observability.md`：固化指标命名规范、指标清单、tag 基数约束、埋点旁路降级约定。
   - 更新 `PRD/novel_aggregator_prd.md` §12 实现进度，追加切片 7 行。
   - 补 `logging-guidelines.md` 若涉及（可选，非必须）。

## Review Gates

- 实现后运行 `trellis-check`。
- 确认无任何业务方法签名/返回/异常语义变更（diff 只增不改逻辑）。
- 确认无无界值进 tag（URL/host/chapterId/异常 message 均不得为 tag value）。
- 确认埋点异常不外抛（review ReadingMetrics 的 try/catch 与埋点位）。
- 确认 pom 未硬编码 prometheus 版本号。

## Rollback Points

- `ReadingMetrics` 为新增类，各埋点位调用可整体移除恢复原状。
- pom 依赖可单独回退；移除后其它 actuator 端点不受影响。
- 无 DB / API / 配置语义变更，无数据回滚需求。
