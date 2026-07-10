# Reading API Contracts

> Executable backend contracts for the DayDayUP reading module user-facing API.

---

## Scenario: Reading Platform v3.0 Redesign Direction

### 1. Scope / Trigger

- Trigger: product direction is reset after competitor analysis and user feedback.
- Applies to: future `daydayup-reading-api` / `daydayup-reading-biz` work after the redesign task.
- Boundary: Open-source reading/Legado book sources are reference material only. Production behavior must be implemented through DayDayUP-owned source configuration, parser, aggregation, sanitization, access-control, health, and distribution capabilities.

### 2. Product Contract Priorities

| Priority | Capability | Contract expectation |
| --- | --- | --- |
| P0/P1 | Multi-source aggregation orchestration | Search/detail/toc/content orchestration must isolate per-source failure, timeout, rate-limit, circuit-break, and disabled-source states. |
| P0/P1 | Per-source observability | Public or ops-facing responses must expose `source_time_cost`-like diagnostics: source id/name, elapsed time, cache hit, result count, status, and disabled/error reason. |
| P1 | Member key and content access limiting | Content access is the control point for `user_key`, membership level, online IP/device, read counters, and ban audit. |
| P1/P2 | Rule/source package distribution | Distribution must publish DayDayUP-owned source/config packages with version and changelog, not raw unaudited external scripts as the production contract. |
| P2 | Lightweight user center and health status | Provide user key/membership/read stats/online device views plus service and source health status. |
| P3 | Multi-modal content and downloads | Comic/audio/drama/client download/EPUB remain outside the first text-novel phase. |

### 3. Wrong vs Correct

#### Wrong

```text
Treat a third-party Legado JSON file as the platform's production executable contract and expose that behavior directly to clients.
```

#### Correct

```text
Use third-party source files as research/reference input, then implement DayDayUP-owned source profiles, parsers, aggregation, sanitization, caching, health checks, and access governance.
```

---

## Scenario: 69shuba First Native Source Integration

### 1. Scope / Trigger

- Trigger: v3.0 uses `https://www.69shuba.com/` as the first concrete native source example.
- Applies to: future SourceProfile, SourceAdapter, Parser, diagnostics, health, cache, and public API integration for `sourceCode=69shuba`.
- Boundary: `docs/书源/69shuba.json` is reference only. Production parser behavior must be proven by directly observed or legally/manual captured fixtures. Current direct fetches returned Cloudflare HTTP 403 challenge, so `blocked` / `verification_required` is a first-class contract.

### 2. Signatures / SourceProfile Contract

```text
sourceCode = 69shuba
sourceName = 69书吧
contentType = text_novel
baseUrl = https://www.69shuba.com
allowedHosts = [www.69shuba.com]
initialStatus = verification_required | degraded
```

```java
SourceSearchResult search(SourceProfile profile, SearchQuery query, SourceRequestContext context);
SourceDiscoveryResult discover(SourceProfile profile, DiscoveryQuery query, SourceRequestContext context);
SourceWorkDetail detail(SourceProfile profile, SourceWorkRef workRef, SourceRequestContext context);
SourceToc toc(SourceProfile profile, SourceWorkRef workRef, SourceRequestContext context);
SourceContent content(SourceProfile profile, SourceChapterRef chapterRef, SourceRequestContext context);
```

| Capability | Source contract | Parser contract |
| --- | --- | --- |
| Search | `POST /modules/article/search.php`, body `searchkey=<GBK keyword>&searchtype=all` | `.newbox li` -> platform `SourceWorkSearchItem` |
| Discovery | `/novels/{sort}_{categoryCode}_{statusCode}_{page}.htm`, plus `/novels/male` and `/novels/female` after fixture verification | `#article_list_content li` -> platform work candidates |
| Detail | `/book/{bookId}.htm`, with `bookId` extracted from an internal source ref | meta properties and `.navtxt` -> `SourceWorkDetail` |
| TOC | Extract href from detail fixture `.more-btn@href` or `.addbtn a:eq(0)@href`; do not hardcode `/txt/{id}/` before verification | `#catalog li a` -> ordered `SourceTocItem` |
| Content | Extract chapter href from TOC fixture | `.txtnav@textNodes` -> raw content, then source cleanup and common sanitizer |

### 3. Contracts

- Challenge detection runs before any business selector. Signals include HTTP `403`, `server: cloudflare`, title `Just a moment...`, `https://challenges.cloudflare.com`, `challenge-platform`, `turnstile`, or `cf-ray`.
- When challenge is detected, adapter returns diagnostic `status=blocked` or `status=verification_required`, `resultCount=0`, `errorReason=cloudflare_challenge`, and safe `httpStatus=403`; it must not treat the page as successful empty results.
- The adapter must not bypass Cloudflare/Turnstile, automate human verification, persist cookies/tokens, or reuse browser verification state.
- Public VOs must never expose upstream URLs, headers, cookies, challenge bodies, source selector strings, or raw source refs.
- Search request body for keyword `斗破苍穹` must be GBK encoded when submitting upstream.
- Status normalization: `完本`, `全本`, or status code `1` -> `completed`; `连载` or status code `2` -> `ongoing`; empty/unknown/status code `0` -> `unknown` unless detail meta confirms status.
- Content cleanup removes markers such as `本章完`, `www.69shuba.com`, `请记住本书首发域名...`, and `loadAdv(...)`; cleanup must preserve Chinese story paragraphs and must not overwrite good sanitized content with empty output.

### 4. Validation & Error Matrix

| Condition | Expected behavior / error |
| --- | --- |
| HTTP 403 Cloudflare challenge on search/list/detail/toc/content | Source diagnostic `blocked` or `verification_required`; upstream 6xxxx category when surfaced; no selector success |
| Search endpoint available but no validated unblocked fixture | Source capability remains `verification_required`; aggregate search may use cached/materialized DayDayUP works only |
| Search keyword encoded as UTF-8 body instead of GBK | Contract test fails before integration is enabled |
| Upstream URL host outside `www.69shuba.com` | Reject by SourceProfile allowed-host / SSRF guard |
| Detail URL not matching verified `/book/{id}.htm` pattern or fixture-derived ref | `READING_INVALID_ARGUMENT` or source ref validation failure |
| TOC URL assumed instead of extracted from detail fixture | Contract review failure; parser not production-ready |
| `.txtnav` missing or text empty | `READING_CONTENT_EMPTY` |
| Cleanup removes all non-empty content | `READING_CONTENT_SANITIZATION_FAILED` or existing content-empty contract; do not publish as good sanitized output |
| Public response contains `sourceBookUrl`, `sourceChapterUrl`, upstream header/cookie, or challenge body | Security contract failure |

### 5. Good / Base / Bad Cases

- Good: challenge fixture with HTTP 403 and `Just a moment...` returns diagnostics `verification_required`, `resultCount=0`, and does not run `.newbox li` / `#catalog` selectors as business HTML.
- Good: `DiscoveryQuery(sort=monthvisit, categoryCode=9, statusCode=2, page=3)` generates `/novels/monthvisit_9_2_3.htm`, but the URL remains internal.
- Good: detail fixture maps meta properties, `.navtxt`, and TOC href into `SourceWorkDetail` and internal TOC ref.
- Base: with only blocked fixtures available, SourceProfile can exist as disabled/degraded behind feature flag, but source is not production-enabled.
- Bad: importing `docs/书源/69shuba.json` and executing it directly as the production parser.
- Bad: adding cookies, Turnstile tokens, or copied browser challenge artifacts to config, fixtures, logs, tests, or distribution packages.
- Bad: returning `https://www.69shuba.com/book/58687.htm` or chapter URLs in any public reading response.

### 6. Tests Required

- Fixture governance: assert no fixture/config contains `Cookie`, `cf_clearance`, Turnstile response tokens, `qttoken`, or reusable browser verification artifacts.
- Challenge handling: assert HTTP 403 + Cloudflare markers maps to `blocked` / `verification_required` and is not parsed as empty successful content.
- Request construction: assert search body for `斗破苍穹` is GBK encoded and includes `searchtype=all`; assert external hosts are rejected.
- Parser fixtures: assert `/book/58687.htm` -> sourceWorkKey `58687`; assert search/list/detail/TOC/content fixtures map into DayDayUP DTOs with normalized status and no public URL exposure.
- Diagnostics and public API: assert every source attempt emits elapsed/cacheHit/resultCount/status/error reason; assert public VOs do not contain upstream URLs, headers, cookies, selector strings, or challenge body.

### 7. Wrong vs Correct

#### Wrong

```java
Document doc = Jsoup.parse(response.body());
List<Element> items = doc.select(".newbox li");
return SourceSearchResult.ok(items.stream().map(this::parseItem).toList());
```

#### Correct

```java
if (challengeDetector.isCloudflareChallenge(response)) {
    return SourceSearchResult.blocked("cloudflare_challenge", response.statusCode());
}
Document doc = htmlParser.parseBusinessHtml(response);
return searchResultParser.parse(doc);
```

#### Wrong

```java
vo.setSourceBookUrl("https://www.69shuba.com/book/58687.htm");
```

#### Correct

```java
vo.setWorkId(workId);
vo.setSourceId(sourceId);
vo.setSourceName("69书吧");
vo.setTitle(detail.title());
```


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
GET /api/v1/reading/sources
GET /api/v1/reading/categories
GET /api/v1/reading/filters
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
ReadingPageVO<ReadingSourceVO> sources(Long workId, int page, int pageSize);
ReadingPageVO<ReadingSourceVO> availableSources(int page, int pageSize);
List<String> categories(int limit);
ReadingFilterVO filters(int limit);
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
- `/sources` returns enabled source summaries only; it must not expose upstream URLs or raw headers.
- `/categories` and `/filters` are metadata endpoints for client-side category display and filtering. `limit` must be bounded (max 100).
- `/works/{workId}/sources` must use database pagination and batch source metadata lookup; do not load all bindings into memory or query `SourceDefinition` one row at a time.

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
