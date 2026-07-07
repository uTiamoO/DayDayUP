# 阅读中台切片7：业务观测性埋点

## Goal

在切片 1~6 已完成主链路（导入→编译→运行时抓取→资产化→净化→统一 API→任务重处理）的基础上，补齐 PRD §8「可观测」要求的**业务维度指标埋点**：让「书源抓取成功率/耗时」「编译等级分布」「净化质量分/结果分布」「任务执行结果」这些运营关键信号从散落的日志与库字段，收敛为可被 Prometheus 采集的 Micrometer 指标，使运营可通过监控看板持续观察平台健康度。

## Background

当前状态（已核实）：

- `application.yml` 的 `management.endpoints.web.exposure.include` 已包含 `prometheus`，但 `micrometer-registry-prometheus` **仅在根 pom 的 `dependencyManagement` 中管理版本，未被任何模块实际引入**，因此 `/actuator/prometheus` 端点实际产不出指标。
- 整个代码库**无任何 Micrometer 业务埋点**（`MeterRegistry`/`Counter`/`Timer`/`@Timed` 全库 0 命中），本切片为全新观测性建设，需同时确立指标命名与埋点约定。
- `MeterRegistry` bean 已通过 `daydayup-common-web` 传递引入的 `spring-boot-starter-actuator` 在容器中可用。
- 关键业务信号目前只以两种非指标形式存在：日志（如 `[rule-compile] 覆盖率 ...`、`[work-assembly] ...`）与落库字段（`ContentSanitizationRun.qualityScore`、`SourceCompiledRule.compileStatus`、`ReadingTaskDrainVO` 计数等）。

## Requirements

- 让 `/actuator/prometheus` 在 reading-biz 真正输出指标：引入 `micrometer-registry-prometheus` 运行时依赖（版本走根 pom 管理，不新增版本号）。
- 覆盖 PRD §8 要求的四类业务维度指标，且不改变既有业务逻辑与返回契约：
  - **书源抓取成功率与耗时**：在运行时唯一 HTTP 出口 `HttpFetcher.fetch()` 采集每次抓取的耗时与结果（成功/超时/被拦截/失败），按书源与结果分维度。
  - **编译等级分布**：在 `RuleCompileService` 编译路径采集 full/degraded/rejected/failed 计数。
  - **净化质量与结果分布**：在 `ContentSanitizeService.sanitize()` 采集净化 runStatus（accepted/degraded/rejected/failed）计数与质量分分布。
  - **任务执行结果**：在 `ReadingTaskWorker.drain()` 采集任务按类型与结果（succeeded/partial/retry/failed）的计数。
- 指标埋点必须是**旁路增量**：不得改变现有方法的返回值、异常语义、事务边界；埋点异常不得影响主流程（指标失败静默降级）。
- 指标命名遵循 Micrometer 约定（点分命名、秒为计时基准单位、维度用 tag 表达），并在 spec 中固化命名规范，供后续切片复用。
- 高基数控制：以书源为维度的 tag 必须有明确的基数评估与约束策略（书源数量级为数十~数百，可接受；须在设计中说明）。
- 不引入独立监控平台部署（Prometheus server / Grafana 部署不在本切片范围），只保证指标可被采集端点正确暴露。

## Non-Goals

- 不做 Prometheus / Grafana 的部署与看板配置（属运维侧，本切片只到「指标可被抓取」为止）。
- 不做分布式链路追踪的改造（Zipkin/Brave 已接入，非本切片范围）。
- 不做告警规则定义。
- 不改动业务逻辑、数据模型、对外 API 契约。
- 不做健康巡检（书源凭据到期 / loginUrl 探活）——那是独立的下一个候选切片。

## Acceptance Criteria

- [ ] `daydayup-reading-biz` 引入 `micrometer-registry-prometheus` 依赖（版本由根 pom 管理，pom 不含硬编码版本号）；`mvn` 依赖解析通过。
- [ ] 启动后 `/actuator/prometheus` 能输出本切片新增的 `reading.*` 指标（本地或测试可验证 registry 中存在对应 meter）。
- [ ] `HttpFetcher.fetch()` 每次调用记录抓取计时与结果维度（success/timeout/blocked/failed），且原有 SSRF、限速、错误码映射行为不变。
- [ ] `RuleCompileService` 编译路径记录 full/degraded/rejected/failed 分级计数。
- [ ] `ContentSanitizeService.sanitize()` 记录 runStatus 计数与质量分分布。
- [ ] `ReadingTaskWorker.drain()` 记录任务按结果维度的计数。
- [ ] 指标埋点异常时主流程不受影响（有对应的静默降级测试或说明）。
- [ ] 新增单元测试验证关键埋点位在既定条件下向 `MeterRegistry` 写入了预期 meter 与 tag（用 `SimpleMeterRegistry` 断言）。
- [ ] 新增/更新 spec：`.trellis/spec/backend/` 下补齐 reading 指标命名与埋点约定文档。
- [ ] Maven 验证通过：`mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test`（使用本机 JDK 21）。

## Notes

- 权威需求来源：`PRD/novel_aggregator_prd.md` §8（可观测：埋点源成功率/耗时/编译等级分布/净化质量，暴露 metrics）。
- 本切片为复杂任务，需配套 `design.md` 与 `implement.md` 后再 start。
- 关键埋点位（已核实文件）：
  - `runtime/http/HttpFetcher.java` — `fetch(FetchRequest)`
  - `compiler/service/impl/RuleCompileServiceImpl.java` — `doCompile` / `compileAllEnabled`
  - `pipeline/service/impl/ContentSanitizeServiceImpl.java` — `sanitize`
  - `task/service/impl/ReadingTaskWorkerImpl.java` — `drain`
