# Design: 阅读中台切片7：业务观测性埋点

## Scope

只做业务维度 Micrometer 埋点与 Prometheus registry 接入，不改任何业务逻辑、数据模型、对外契约。埋点为旁路增量：在既有 service 方法内部记录 meter，方法签名、返回值、异常、事务边界均不变。

## Dependency Wiring

`MeterRegistry` bean 已由 `daydayup-common-web` → `spring-boot-starter-actuator` 传递提供，容器内可直接注入。但要让 `/actuator/prometheus`（已在 `application.yml` 暴露）真正产出文本，需要 Prometheus registry 实现。

在 `daydayup-reading-biz/pom.xml` 增加：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

版本由根 pom `dependencyManagement` 管理（当前 `1.13.6`），pom 不写死版本号。这是运行时依赖，`scope` 默认 compile 即可（Spring Boot 会自动装配 `PrometheusMeterRegistry`）。

> 说明：`micrometer-registry-prometheus` 在根 pom 只被 `dependencyManagement` 声明，此前无任何模块实际引入。本切片是首个真正启用 Prometheus 指标输出的模块。

## Metric Naming Convention

遵循 Micrometer 官方约定，供后续所有 reading 切片复用：

- 命名用点分小写：`reading.<domain>.<subject>`。
- 计时用 `Timer`，基准单位秒（Micrometer 自动转换，Prometheus 端呈现 `_seconds`）。
- 计数用 `Counter`。
- 分布/摘要（如质量分）用 `DistributionSummary`。
- 维度一律用 tag 表达，不塞进 metric 名。
- tag key 用小写点分或短横；tag value 必须是**有界枚举**，禁止把无界值（如 URL、异常 message、chapterId）放进 tag。

### 指标清单

| Metric | 类型 | Tags | 含义 |
| --- | --- | --- | --- |
| `reading.fetch.duration` | Timer | `source`, `result`(success/timeout/blocked/failed) | 单次源站抓取耗时与结果 |
| `reading.compile.total` | Counter | `grade`(full/degraded/rejected/failed) | 编译分级计数 |
| `reading.sanitize.total` | Counter | `result`(accepted/degraded/rejected/failed) | 净化结果计数 |
| `reading.sanitize.quality` | DistributionSummary | — | 净化质量分分布（0~100） |
| `reading.task.total` | Counter | `type`(taskType), `result`(succeeded/partial/retry/failed) | 任务执行结果计数 |

`result`/`grade`/`type` 均为已存在的有界枚举，基数可控。

## Cardinality Control

唯一有基数风险的是 `reading.fetch.duration` 的 `source` tag：

- 书源数量级为数十~数百（PRD 覆盖率普查基于 68% 样本，全量导入亦在数百级），作为 tag 基数可接受。
- `source` tag value 用**书源稳定标识**（`sourceKey` = bookSourceUrl/identity.key，或映射为 sourceId 字符串），不是随机值，集合有界。
- 明确禁止：把目标 URL、host、章节 ID、异常 message 作为 tag。
- 若未来书源规模上升到数千级，可在配置中降级为「不带 source tag，仅按 result 维度」——设计上把 tag 组装收敛到单一辅助方法，便于后续调整。

## Instrumentation Approach

新增一个薄封装组件 `observability/ReadingMetrics`（`@Component`），集中持有 `MeterRegistry` 并暴露语义化记录方法，避免埋点代码散落、并统一「静默降级」策略：

```java
@Component
public class ReadingMetrics {
    private final MeterRegistry registry;
    // recordFetch(sourceKey, result, durationNanos)
    // recordCompile(grade)
    // recordSanitize(result, qualityScore)
    // recordTask(type, result)
}
```

要点：

- 每个记录方法内部 try/catch 兜底，埋点异常只记 debug 日志，绝不外抛（旁路不影响主流程）。
- 计时优先用 `Timer.record(Duration)` 或手动 `sample`，不用 `@Timed`（`@Timed` 依赖 AOP 且难以表达 result 维度）。
- 各埋点位注入 `ReadingMetrics`，而非直接注入 `MeterRegistry`，保证约定集中。

### 各埋点位改造

1. **`HttpFetcher.fetch()`** —— 在方法入口取 `System.nanoTime()`，在四个出口（成功 return / timeout / blocked / failed）分别以对应 `result` 调 `recordFetch`。注意 `blocked`（SSRF `READING_UPSTREAM_BLOCKED`/60103）与 `failed`/`timeout` 的区分：SSRF 校验在 `ssrf.validate` 抛 `BizException(READING_UPSTREAM_BLOCKED)`，需在 fetch 内用 try/finally + 结果标记捕获分类。埋点包裹**不改变**现有 SSRF/限速/重定向/错误码逻辑。

2. **`RuleCompileServiceImpl`** —— `doCompile` 成功后按 `grade` 调 `recordCompile(grade)`；`compileAllEnabled` 的 catch 分支（编译抛异常）调 `recordCompile("failed")`。单源编译 `compileOne` 也覆盖。

3. **`ContentSanitizeServiceImpl.sanitize()`** —— pipeline 异常 archive failed 后调 `recordSanitize("failed", null)`；正常路径按 `result.getRunStatus()`（accepted/degraded/rejected）调 `recordSanitize(status, qualityScore)`，同时记质量分分布。

4. **`ReadingTaskWorkerImpl.drain()`** —— 在现有 succeeded/partial/retry/failed 分支各调 `recordTask(task.getTaskType(), result)`。`taskType` 为有界枚举（6 类），可安全作 tag。

## Data Flow

```
业务方法执行 ──(旁路)──> ReadingMetrics.recordXxx() ──> MeterRegistry(Prometheus)
                                                              │
                                              /actuator/prometheus 文本输出
                                                              │
                                              (运维侧) Prometheus server 抓取  ← 本切片不含
```

主数据流（抓取/编译/净化/任务）完全不变；指标是只读旁路。

## Tradeoffs

- **薄封装 vs 直接埋点**：选薄封装（`ReadingMetrics`），换取命名集中、降级统一、后续调 tag 方便，代价是多一层间接。相较散落埋点，可维护性更高。
- **`Timer` vs `@Timed` 注解**：选编程式 `Timer`。`@Timed` 无法自然表达运行时决定的 `result` 维度，且引入 AOP 依赖。
- **source tag 基数**：接受当前数百级基数换取按源诊断能力；预留「关闭 source tag」的降级路径。
- **不碰 metrics 部署**：本切片只保证「端点能出指标」，Prometheus/Grafana 部署交运维，避免范围膨胀。

## Compatibility & Rollback

- 纯增量：新增 1 个依赖 + 1 个组件 + 4 处旁路调用。不改表、不改 API、不改配置语义（`application.yml` 的 prometheus 暴露此前就在）。
- 回滚：移除 `ReadingMetrics` 注入与调用即恢复原状；移除依赖后 `/actuator/prometheus` 回到「无 registry」状态（不影响其它端点）。
- 埋点全程 try/catch 降级，最坏情况指标缺失，不影响主链路可用性。

## Tests

- `ReadingMetricsTest`：用 `SimpleMeterRegistry` 注入，断言各 record 方法写入预期 meter 名与 tag；断言传入异常参数时静默不抛。
- `HttpFetcherTest`（既有，扩充）：mockwebserver 场景下断言成功/超时/失败各写入 `reading.fetch.duration` 对应 result tag；SSRF 拦截场景断言 `result=blocked`。
- `RuleCompileServiceTest` / `ContentSanitizeServiceTest` / `ReadingTaskWorkerTest`：在既有测试基础上注入 `SimpleMeterRegistry`（或 mock ReadingMetrics 验证调用），断言对应计数被记录。
- 不新增集成测试拉起 `/actuator/prometheus`（成本高）；以 registry 断言覆盖「meter 已注册」即可，端点输出由 Spring Boot 自动装配保证。
