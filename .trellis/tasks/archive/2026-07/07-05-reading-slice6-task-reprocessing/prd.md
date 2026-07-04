# 阅读中台切片6：同步任务与重处理闭环

## Goal

在切片 1~5 已完成同步调用闭环的基础上，补齐第一期「应用内任务 + 数据库任务表 + 手动触发 + 简单 worker」能力，让导入、编译、内容发现、目录同步、正文补抓和净化重跑可以通过任务入库、状态查询、失败重试与 worker 消费持续运转。

## Requirements

- 新增阅读任务表与实体/Mapper，任务必须支持：
  - 任务类型：source_import、source_compile、work_discovery、toc_sync、content_fetch、content_sanitize。
  - 状态：pending、running、succeeded、failed、partial_succeeded、cancelled。
  - 幂等键：同一 taskType + bizKey 在 pending/running 下只能存在一个活动任务。
  - payload JSON：保存任务执行所需参数。
  - retryCount/maxRetry/nextRunAt/lockedBy/lockedAt/errorMessage/startedAt/finishedAt。
- 新增任务服务，支持手动提交、查询详情、分页查询、取消 pending 任务、worker 抢占待执行任务、标记成功/失败。
- 新增任务执行器，按任务类型调用既有同步服务：
  - source_import：调用 `SourceImportService`。
  - source_compile：调用 `RuleCompileService`。
  - work_discovery：调用 `ContentDiscoveryService`。
  - toc_sync：调用 `ChapterSyncService`。
  - content_fetch：调用 `ContentFetchService`。
  - content_sanitize：调用 `ContentSanitizeService`。
- worker 必须使用数据库抢占语义，避免多实例重复执行；锁超时任务可被重新抢占。
- 失败任务在 retryCount < maxRetry 时进入 pending 并延后 nextRunAt；达到 maxRetry 后进入 failed。
- 新增内网 ops API，用于提交任务、查询任务、触发一次 worker drain。
- 任务执行必须复用既有服务与 parse-runtime 限速能力，不允许绕过 `HttpFetcher` / `SourceRateLimiter` 直接抓取。
- 第一版不引入 MQ、BPM 或独立调度平台；定时触发仅做可配置的应用内简单轮询。

## Acceptance Criteria

- [ ] 新增 `reading_task` 表 SQL、`ReadingTask` 实体、`ReadingTaskMapper`。
- [ ] `ReadingTaskService` 支持 submit/get/page/cancel/acquire/success/failure。
- [ ] 同一 `taskType + bizKey` 的 pending/running 任务重复提交时返回既有任务，不重复入库。
- [ ] worker 抢占 pending/到期任务时写入 `running + lockedBy + lockedAt + startedAt`，并跳过未到 `nextRunAt` 的任务。
- [ ] 执行成功后任务变为 succeeded 或 partial_succeeded，并记录 finishedAt。
- [ ] 执行失败后按 maxRetry 重试；超过 maxRetry 后任务为 failed，并记录 errorMessage。
- [ ] 内网 ops API 暴露提交任务、查询详情、分页查询、取消任务、手动 drain 接口。
- [ ] 覆盖任务幂等、抢占、成功、失败重试、取消、至少 3 类任务 handler 分发的单元测试。
- [ ] Maven 验证通过：`mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test`（使用本机 JDK 21）。

## Notes

- 权威需求来源：`PRD/novel_aggregator_prd.md` §10/§12，以及 `docs/superpowers/specs/2026-07-02-reading-api-platform-design.md` §8/§10.7。
- 本切片为复杂任务，需配套 `design.md` 与 `implement.md` 后再 start。
