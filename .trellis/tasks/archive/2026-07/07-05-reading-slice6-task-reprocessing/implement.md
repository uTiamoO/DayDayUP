# Implementation Plan: 阅读中台切片6：同步任务与重处理闭环

## Pre-check

- 当前任务：`.trellis/tasks/07-05-reading-slice6-task-reprocessing`。
- 阅读顺序：`implement.jsonl` → `prd.md` → `design.md` → 本文件。
- Maven 使用 JDK 21：`JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9`。

## Steps

1. **补齐任务 API VO/DTO**
   - 在 `daydayup-reading-api` 添加任务提交 DTO/查询 DTO/任务 VO/worker drain VO。
   - payload 可先用 `String payload` 或 `Map<String,Object>`，内部统一序列化为 JSON。

2. **新增任务表 SQL 与领域模型**
   - 更新 `sql/daydayup_reading.sql`，增加 `reading_task`。
   - 新增 `ReadingTask` 实体。
   - 新增 `ReadingTaskType` / `ReadingTaskStatus` 常量或 enum。
   - 新增 `ReadingTaskMapper`：活动任务查询、分页查询、待抢占查询、条件抢占更新、状态更新。

3. **实现任务服务**
   - `submit`：先查同 type+bizKey 的 pending/running；存在则返回既有任务，否则插入 pending。
   - `get/page/cancel`。
   - `acquireOne`：实现 pending 到期与 running 锁超时抢占。
   - `markSucceeded/markPartial/markFailed`。

4. **实现执行器**
   - JSON payload 解析工具方法，缺字段抛 `READING_INVALID_ARGUMENT`。
   - 分发到既有服务：import/compile/discover/toc/fetch/sanitize。
   - 执行器不直接 HTTP 抓取。

5. **实现 worker**
   - `drain(limit)`：循环 acquire + execute + mark。
   - 可选 `@Scheduled`，受 `reading.task.worker-enabled` 控制。
   - workerId 使用应用名 + 本机标识，简单即可。

6. **新增 ops Controller**
   - `/api/v1/internal/ops/tasks/submit`
   - `/api/v1/internal/ops/tasks/{taskId}`
   - `/api/v1/internal/ops/tasks/page`
   - `/api/v1/internal/ops/tasks/{taskId}/cancel`
   - `/api/v1/internal/ops/tasks/drain`

7. **配置**
   - 添加 `ReadingTaskProperties`。
   - 更新 reading `application.yml` 默认关闭自动 worker。

8. **测试**
   - `ReadingTaskServiceTest`：submit 幂等、cancel、acquire、success/failure retry。
   - `ReadingTaskExecutorTest`：至少 3 类 handler 分发。
   - `ReadingTaskWorkerTest`：drain 结果统计。
   - 运行：
     ```bash
     JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test
     ```

9. **更新进度文档和 code-spec**
   - 更新 `PRD/novel_aggregator_prd.md` §12 切片 6 状态。
   - 在 `.trellis/spec/backend/reading-api-contracts.md` 或新增 reading task spec 记录任务 API/状态机契约。

## Review Gates

- 实现后运行 `trellis-check`。
- 检查 task API 仍在 `/api/v1/internal/**`，网关不暴露。
- 检查任务执行没有绕过 runtime 限速。
- 检查失败重试不会无限循环。

## Rollback Points

- 任务 package 是新增，可整体回滚。
- SQL 新增表独立，不影响既有表。
- 自动 worker 默认关闭，若出现风险可仅保留手动 drain。
