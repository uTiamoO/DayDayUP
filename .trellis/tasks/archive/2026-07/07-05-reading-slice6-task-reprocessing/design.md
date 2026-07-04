# Design: 阅读中台切片6：同步任务与重处理闭环

## Scope

切片 6 只补齐任务化与重处理骨架，不改变切片 1~5 已完成的同步服务语义。任务执行器通过既有 service 编排执行，因此所有抓取仍走运行时 HTTP 出口、SSRF 防护与 per-source 限速。

## Data Model

新增表：`reading_task`。

核心字段：

- `id`：雪花主键。
- `task_type`：`source_import` / `source_compile` / `work_discovery` / `toc_sync` / `content_fetch` / `content_sanitize`。
- `biz_key`：同类型幂等键。
- `payload`：JSON 参数。
- `task_status`：`pending` / `running` / `succeeded` / `failed` / `partial_succeeded` / `cancelled`。
- `retry_count` / `max_retry`。
- `next_run_at`。
- `locked_by` / `locked_at`。
- `started_at` / `finished_at`。
- `error_message`。
- BaseEntity 字段：create_time/update_time/create_by/update_by/deleted。

唯一性：MySQL 无法直接对「pending/running」做部分唯一索引。第一版使用 service 层先查活动任务再插入；数据库加普通联合索引 `(task_type,biz_key,task_status,deleted)`，降低重复窗口。后续如需要强一致，可引入 generated active_key。

## Package Layout

新增 package：`com.yuan.daydayup.reading.task`。

建议结构：

```text
task/
  config/ReadingTaskProperties.java
  controller/ReadingTaskOpsController.java
  entity/ReadingTask.java
  mapper/ReadingTaskMapper.java
  model/ReadingTaskStatus.java
  model/ReadingTaskType.java
  service/ReadingTaskService.java
  service/ReadingTaskExecutor.java
  service/ReadingTaskWorker.java
  service/impl/ReadingTaskServiceImpl.java
  service/impl/ReadingTaskExecutorImpl.java
  service/impl/ReadingTaskWorkerImpl.java
```

## API Boundary

内网 ops API：`/api/v1/internal/ops/tasks`。

- `POST /submit`
  - 参数：`taskType,bizKey,payload,maxRetry,nextRunAt`。
  - 返回任务摘要。
- `GET /{taskId}`：任务详情。
- `GET /page`：按 `taskType,status,page,pageSize` 查询。
- `POST /{taskId}/cancel`：仅 pending 可取消。
- `POST /drain?limit=10`：手动执行一批到期任务，便于本地/内网验证。

第一版使用 `Map<String,Object>` 或 JSON 字符串作为 payload 入参即可，避免过早创建每类任务 DTO。

## Worker / Locking

抢占规则：

1. 查询 `pending` 且 `nextRunAt <= now` 的任务，或 `running` 但 `lockedAt` 早于锁超时时间的任务。
2. 按 `nextRunAt ASC, id ASC` 取一条。
3. 用条件更新抢占：只有当前状态仍符合条件时，将任务置为 `running`，写 `lockedBy/lockedAt/startedAt`。
4. 条件更新成功后才执行；失败则继续尝试下一条。

执行结果：

- success：置 `succeeded`，写 `finishedAt`，清理 error。
- partial：置 `partial_succeeded`，写 `finishedAt`。
- failure：
  - 若 `retryCount + 1 < maxRetry`，置 `pending`，`retryCount+1`，`nextRunAt=now+backoff`，记录 error。
  - 否则置 `failed`，写 `finishedAt/errorMessage`。

## Payload Contracts

- `source_import`
  - `sourceJson` 或 `dir`。第一版优先支持复用现有 import JSON 字符串入口；批量目录扫描可保留为后续扩展。
- `source_compile`
  - `sourceId`，可选 `all=true`。
- `work_discovery`
  - `sourceId`, `keyword`, `page`。
- `toc_sync`
  - `workId`, `sourceId`。
- `content_fetch`
  - `chapterId`, `sourceId`, `forceRefresh`。
- `content_sanitize`
  - `chapterId`, `sourceId`。

## Error Handling

- payload 缺字段：`READING_INVALID_ARGUMENT`。
- 任务不存在：`READING_INVALID_ARGUMENT`（第一版不新增错误码）。
- handler 内部业务异常：记录 errorMessage，并按 retry 策略处理。
- cancelled 任务不可执行；非 pending 任务不可取消。

## Configuration

新增配置：

```yaml
reading:
  task:
    worker-enabled: ${READING_TASK_WORKER_ENABLED:false}
    worker-interval-ms: ${READING_TASK_WORKER_INTERVAL_MS:5000}
    lock-timeout-ms: ${READING_TASK_LOCK_TIMEOUT_MS:300000}
    default-max-retry: ${READING_TASK_DEFAULT_MAX_RETRY:3}
    retry-backoff-ms: ${READING_TASK_RETRY_BACKOFF_MS:30000}
    batch-size: ${READING_TASK_BATCH_SIZE:10}
```

默认关闭自动 worker，避免本地启动后意外抓取；通过手动 drain 验证。

## Tests

- service submit 幂等。
- acquire 跳过未到期任务，并能抢占锁超时 running 任务。
- success/failure 状态转换。
- cancel 只允许 pending。
- executor 分发至少覆盖 `source_compile`、`toc_sync`、`content_fetch/content_sanitize`。
- controller 可轻量测试参数绑定，或以 service 单测为主。
