# Reading API Contracts

> Executable backend contracts for the DayDayUP reading module user-facing API.

---

## Scenario: Unified Reading API Slice 5

### 1. Scope / Trigger

- Trigger: user-facing REST API and gateway route added for the reading module.
- Applies to: `daydayup-modules/daydayup-reading/daydayup-reading-api`, `daydayup-reading-biz`, and gateway `/reading/**` routing.
- Boundary: public reading APIs expose platform IDs and normalized reading assets only. Source-site URLs stay internal to runtime/repository bindings.

### 2. Signatures

Public controller prefix:

```http
GET /api/v1/reading/search
GET /api/v1/reading/works
GET /api/v1/reading/works/{workId}
GET /api/v1/reading/works/{workId}/sources
GET /api/v1/reading/works/{workId}/chapters
GET /api/v1/reading/chapters/{chapterId}/content
```

Gateway route:

```yaml
- id: daydayup-reading
  uri: lb://daydayup-reading-biz
  predicates:
    - Path=/reading/**
  filters:
    - StripPrefix=1
```

Service signatures:

```java
ReadingPageVO<ReadingWorkVO> search(String keyword, int page, int pageSize,
                                    String mode, Long sourceId,
                                    String category, String completionStatus);
ReadingPageVO<ReadingWorkVO> works(String category, String status, String sort,
                                   Long sourceId, int page, int pageSize);
ReadingWorkDetailVO detail(Long workId);
List<ReadingSourceVO> sources(Long workId);
ReadingPageVO<ReadingChapterVO> chapters(Long workId, Long sourceId,
                                         int page, int pageSize, String refreshPolicy);
ReadingContentVO content(Long chapterId, Long sourceId,
                         String contentVersion, String fetchPolicy);
```

### 3. Contracts

Search request fields:

| Field | Type | Required | Contract |
| --- | --- | --- | --- |
| `keyword` | string | no | Matches title/author in aggregate mode; passed to source discovery in source mode. |
| `mode` | string | no | `aggregate` by default; allowed: `aggregate`, `source`. |
| `sourceId` | long | source mode only | Required when `mode=source`; optional filter for aggregate/listing. |
| `category` | string | no | Filters aggregate read-model query. |
| `completionStatus` | string | no | Filters aggregate read-model query. |
| `page` / `pageSize` | int | no | Returned as `ReadingPageVO`; normalize or reject invalid values in service. |

Chapter request fields:

| Field | Type | Required | Contract |
| --- | --- | --- | --- |
| `sourceId` | long | no | When present, marks source availability for each unified chapter. |
| `refreshPolicy` | string | no | `cache-first` by default; `force-refresh` requires `sourceId` and runs TOC sync before returning. |

Content request fields:

| Field | Type | Required | Contract |
| --- | --- | --- | --- |
| `sourceId` | long | yes | Public content lookup is `chapterId + sourceId`. |
| `contentVersion` | string | no | Allowed: `latest`, `sanitized`, `normalized`, `raw`; `latest` means `sanitized`. |
| `fetchPolicy` | string | no | Allowed: `cache-first`, `force-refresh`. |

Response contracts:

- All controller responses use `R<T>`.
- Pagination uses `ReadingPageVO<T>` with `list,total,page,pageSize,hasNext`.
- Public VOs must not expose `sourceBookUrl` or `sourceChapterUrl`.
- Content defaults to sanitized output; raw/normalized are available only by explicit version request.

### 4. Validation & Error Matrix

| Condition | Expected error |
| --- | --- |
| Unsupported `mode` | `READING_UNSUPPORTED_MODE` |
| `mode=source` without `sourceId` | `READING_INVALID_ARGUMENT` |
| Unsupported `refreshPolicy` | `READING_INVALID_ARGUMENT` |
| `refreshPolicy=force-refresh` without `sourceId` | `READING_INVALID_ARGUMENT` |
| Unsupported `contentVersion` | `READING_INVALID_ARGUMENT` |
| Unsupported `fetchPolicy` | `READING_INVALID_ARGUMENT` |
| Missing work | `READING_WORK_NOT_FOUND` |
| Missing chapter/source binding | `READING_SOURCE_NOT_AVAILABLE` |
| Empty fetched content | `READING_CONTENT_EMPTY` |
| Sanitization failure | `READING_CONTENT_SANITIZATION_FAILED` |
| Upstream timeout/blocked/fetch failure | Propagate reading 6xxxx upstream error codes. |

### 5. Good/Base/Bad Cases

- Good: `GET /api/v1/reading/search?mode=source&sourceId=10&keyword=斗破&page=1&pageSize=20` discovers from one source, materializes works, and returns platform work IDs.
- Base: `GET /api/v1/reading/chapters/{workId}?refreshPolicy=cache-first` returns existing unified chapters without upstream calls.
- Good: `GET /api/v1/reading/chapters/{chapterId}/content?sourceId=10` returns sanitized content; if sanitized is missing but raw/normalized exists, sanitize cached content before refetching upstream.
- Bad: returning `sourceBookUrl` or `sourceChapterUrl` in any public VO leaks SSRF-sensitive implementation details.
- Bad: adding a gateway route for `/api/v1/internal/**` exposes operations and source-reading endpoints externally.

### 6. Tests Required

- Source search:
  - Assert `mode=source` calls `ContentDiscoveryService.discover(sourceId, keyword, page)`.
  - Assert response uses `ReadingPageVO<ReadingWorkVO>` and platform IDs.
- Aggregate search:
  - Assert `category` and `completionStatus` reach the read-model query.
- Chapters:
  - Assert `force-refresh` with `sourceId` calls `ChapterSyncService.syncToc` before listing.
  - Assert unsupported refresh policy fails with `READING_INVALID_ARGUMENT`.
- Content:
  - Assert default version is sanitized/latest.
  - Assert cache-first sanitizes existing raw/normalized content without unnecessary fetch.
  - Assert force-refresh calls fetch then sanitize.
- Gateway:
  - Assert `/reading/**` StripPrefix route exists.
  - Assert no `/api/v1/internal/**` route is added.

### 7. Wrong vs Correct

#### Wrong

```java
// Public API response leaks source-site URLs; clients can replay arbitrary source URLs.
vo.setSourceBookUrl(binding.getSourceBookUrl());
vo.setSourceChapterUrl(binding.getSourceChapterUrl());
```

#### Correct

```java
// Public API exposes only platform IDs and safe source metadata.
vo.setWorkId(binding.getWorkId());
vo.setSourceId(binding.getSourceId());
vo.setSourceName(binding.getSourceBookName());
vo.setPrimarySource(binding.getIsPrimarySource() != null && binding.getIsPrimarySource() == 1);
```

#### Wrong

```java
// Missing sanitized content always refetches upstream, even when raw/normalized cache exists.
if (!StringUtils.hasText(snapshot.getSanitizedContent())) {
    contentFetchService.fetchAndStore(chapterId, sourceId, false);
}
```

#### Correct

```java
// Cache-first can sanitize existing cached raw/normalized content without refetch.
if (!StringUtils.hasText(snapshot.getSanitizedContent())
        && !StringUtils.hasText(snapshot.getRawContent())
        && !StringUtils.hasText(snapshot.getNormalizedContent())) {
    contentFetchService.fetchAndStore(chapterId, sourceId, false);
}
contentSanitizeService.sanitize(chapterId, sourceId);
```

---

## Scenario: Reading Task/Reprocessing Slice 6

### 1. Scope / Trigger

- Trigger: application-internal task table, worker, and ops APIs are added for the reading module.
- Applies to: `daydayup-reading-api` task DTO/VOs and `daydayup-reading-biz` `task/**` package.
- Boundary: task APIs are internal-only under `/api/v1/internal/**`; gateway must not expose them.

### 2. API Signatures

```http
POST /api/v1/internal/ops/tasks/submit
GET  /api/v1/internal/ops/tasks/{taskId}
GET  /api/v1/internal/ops/tasks/page
POST /api/v1/internal/ops/tasks/{taskId}/cancel
POST /api/v1/internal/ops/tasks/drain?limit=10
```

### 3. Task Contracts

Allowed task types:

- `source_import`
- `source_compile`
- `work_discovery`
- `toc_sync`
- `content_fetch`
- `content_sanitize`

Allowed statuses:

- `pending`
- `running`
- `succeeded`
- `failed`
- `partial_succeeded`
- `cancelled`

Submit is idempotent for active tasks: a duplicate `taskType + bizKey` while an existing task is `pending` or `running` returns that existing task instead of inserting another row.

Worker acquire must use database conditional update semantics. It may acquire due `pending` tasks and stale-lock `running` tasks only; it must skip tasks whose `nextRunAt` is in the future.

Task handlers must call existing services (`SourceImportService`, `RuleCompileService`, `ContentDiscoveryService`, `ChapterSyncService`, `ContentFetchService`, `ContentSanitizeService`) and must not bypass `HttpFetcher` / `SourceRateLimiter` with direct HTTP calls.

### 4. Validation & Error Matrix

| Condition | Expected error |
| --- | --- |
| Unsupported `taskType` | `READING_INVALID_ARGUMENT` |
| Missing required payload field | `READING_INVALID_ARGUMENT` |
| Missing task | `READING_INVALID_ARGUMENT` |
| Cancel non-`pending` task | `READING_INVALID_ARGUMENT` |
| Handler business failure with retries left | task returns to `pending` with incremented `retryCount` and delayed `nextRunAt` |
| Handler business failure after retries exhausted | task becomes `failed` with `finishedAt/errorMessage` |

### 5. Good/Base/Bad Cases

- Good: submitting `taskType=content_fetch` with the same `bizKey=chapter:1:source:10` while an existing task is `pending` returns the existing task row instead of inserting a duplicate.
- Good: manual `POST /api/v1/internal/ops/tasks/drain?limit=10` acquires due tasks using database conditional update, executes through existing services, and records counts for succeeded, partial, retry-scheduled, and failed tasks.
- Base: automatic worker stays disabled by default with `reading.task.worker-enabled=false`; local startup must not begin fetching upstream content unless explicitly enabled.
- Bad: task executor directly creates an HTTP client or calls source URLs itself; this bypasses SSRF checks and per-source rate limiting.
- Bad: stale-lock reacquire preserves the previous `startedAt`; the new execution attempt must update `startedAt`, `lockedBy`, and `lockedAt` for auditability.

### 6. Tests Required

- Task service:
  - Assert duplicate active `taskType + bizKey` returns the existing task.
  - Assert `cancel` succeeds only for `pending` tasks.
  - Assert acquire skips future `nextRunAt` and can reacquire stale `running` locks.
  - Assert stale-lock reacquire updates `startedAt` to the new attempt timestamp.
  - Assert failures schedule delayed retry while retry budget remains and mark final `failed` after exhaustion.
- Task executor:
  - Assert `source_import`, `source_compile`, `work_discovery`, `toc_sync`, `content_fetch`, and `content_sanitize` dispatch to their existing services.
  - Assert missing required payload fields throw `READING_INVALID_ARGUMENT`.
- Worker:
  - Assert drain records success, partial success, retry-scheduled failure, and final failure counts.
- Boundary:
  - Assert task ops controller path remains under `/api/v1/internal/ops/tasks`.
  - Assert gateway config does not add a `/api/v1/internal/**` route.

### 7. Wrong vs Correct

#### Wrong

```java
// Reacquired stale running task keeps the old attempt timestamp.
@Update("UPDATE reading_task SET task_status = 'running', locked_by = #{workerId}, "
        + "locked_at = #{now}, started_at = COALESCE(started_at, #{now}) WHERE id = #{id}")
int tryAcquire(...);
```

#### Correct

```java
// Every acquired execution attempt writes fresh lock and start timestamps.
@Update("UPDATE reading_task SET task_status = 'running', locked_by = #{workerId}, "
        + "locked_at = #{now}, started_at = #{now}, finished_at = NULL WHERE id = #{id}")
int tryAcquire(...);
```

#### Wrong

```java
// Executor bypasses reading runtime safeguards.
String body = new OkHttpClient().newCall(new Request.Builder().url(sourceUrl).build()).execute().body().string();
```

#### Correct

```java
// Executor delegates to existing services; runtime HTTP, SSRF and rate limiting stay centralized.
contentFetchService.fetchAndStore(chapterId, sourceId, forceRefresh);
contentSanitizeService.sanitize(chapterId, sourceId);
```
