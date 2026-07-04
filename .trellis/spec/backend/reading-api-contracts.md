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
