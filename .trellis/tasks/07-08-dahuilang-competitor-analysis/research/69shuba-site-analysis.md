# Research: 69shuba Site Analysis

- **Query**: Analyze `https://www.69shuba.com/` as the first concrete DayDayUP reading v3.0 source; use `docs/书源/69shuba.json` only as reference; base parsing plan on directly observed website/API behavior where possible.
- **Scope**: mixed
- **Date**: 2026-07-09

## Findings

### Files Found

| File Path | Description |
|---|---|
| `docs/书源/69shuba.json` | Reference Legado-style source definition for 69shuba selectors, URL patterns, headers, charset, and known Cloudflare/Turnstile caveat. |
| `.trellis/spec/backend/reading-api-contracts.md` | DayDayUP reading v3.0 contract priorities and public API constraints, including per-source diagnostics and no public source URL leakage. |

### Direct Fetch Observations

Direct public fetches were attempted from this environment with a normal browser-like `User-Agent`, `Accept`, `Accept-Language: zh-CN,zh;q=0.9`, and `Referer: https://www.69shuba.com/` headers. No cookies or tokens were used or stored.

| Page Type | URL Tested | Result | Observed Headers / Body |
|---|---|---|---|
| Homepage | `https://www.69shuba.com/` | Blocked | HTTP `403`, `server: cloudflare`, `content-type: text/html; charset=UTF-8`, body title `Just a moment...`, CSP references `https://challenges.cloudflare.com`. |
| Category/list page | `https://www.69shuba.com/novels/monthvisit_0_0_1.htm` | Blocked | HTTP `403`, Cloudflare challenge body. |
| Category/list page | `https://www.69shuba.com/novels/male` | Blocked | HTTP `403`, Cloudflare challenge body. |
| Book detail page | `https://www.69shuba.com/book/58687.htm` | Blocked | HTTP `403`, Cloudflare challenge body. |
| TOC page candidate | `https://www.69shuba.com/txt/58687/` | Blocked | HTTP `403`, Cloudflare challenge body. |
| Chapter content page candidate | `https://www.69shuba.com/book/58687/25305874.html` | Blocked | HTTP `403`, Cloudflare challenge body. |
| Search endpoint, GET | `https://www.69shuba.com/modules/article/search.php` | Blocked | HTTP `403`, Cloudflare challenge body. |
| Search endpoint, POST | `https://www.69shuba.com/modules/article/search.php` with GBK body `searchkey=斗破苍穹&searchtype=all` | Blocked | HTTP `403`, Cloudflare challenge body. |
| Robots | `https://www.69shuba.com/robots.txt` | Blocked | HTTP `403`, Cloudflare challenge body. |

Observed challenge markers that should be detected as upstream block/verification states:

```text
HTTP 403
server: cloudflare
<title>Just a moment...</title>
https://challenges.cloudflare.com
challenge-platform
turnstile
cf-ray: <present in responses>
```

### URL Patterns

Directly tested patterns and reference-derived patterns align on the following contracts, but the HTML structure behind them could not be confirmed because every public page tested returned Cloudflare challenge HTML.

| Capability | Pattern | Evidence | Notes |
|---|---|---|---|
| Base source | `https://www.69shuba.com` | Reference + tested | All tested paths are on this host. |
| Book detail | `/book/{bookId}.htm` | Reference `bookUrlPattern` + tested `58687` | Reference regex: `https?://www\.69shuba\.com/book/\d+\.htm`. |
| TOC | From detail `class.more-btn@href` or `class.addbtn a:first@href`; tested candidate `/txt/{bookId}/` | Reference + tested candidate | Actual href must be learned from unblocked detail HTML. Do not hardcode `/txt/{id}/` until verified per book. |
| Chapter content | Chapter hrefs under TOC anchors; tested candidate `/book/{bookId}/{chapterId}.html` | Reference selectors + tested candidate | Actual pattern must be learned from unblocked TOC. |
| Search | `/modules/article/search.php` | Reference + tested | POST form body uses `searchkey=<GBK keyword>&searchtype=all`; tested direct POST was blocked. |
| Ranking/list | `/novels/{sort}_{categoryCode}_{statusCode}_{page}.htm` | Reference | Example reference paths: `/novels/monthvisit_0_0_{{page}}.htm`, `/novels/allvote_0_0_{{page}}.htm`. |
| Male/female discovery | `/novels/male`, `/novels/female` | Reference + tested | Tested direct access was blocked. |

### Encodings and Headers

Reference selectors indicate mixed encoding requirements:

| Request Type | Charset / Headers | Evidence |
|---|---|---|
| Normal HTML pages | Response challenge observed as `charset=UTF-8`; target page charset unconfirmed while blocked. | Direct fetches only reached Cloudflare HTML. |
| Search POST | Request body charset should be `gbk`. | `docs/书源/69shuba.json` searchUrl: `{"method":"POST","body":"searchkey={{...}}&searchtype=all","charset":"gbk"}`. |
| Common headers | `Referer: https://www.69shuba.com/`, `Accept-Language: zh-CN,zh;q=0.9`. | Reference `header` object. Browser-like headers alone did not avoid Cloudflare. |

### Reference Selectors to Validate on Unblocked HTML

These selectors are from `docs/书源/69shuba.json`; treat them as hypotheses until DayDayUP captures unblocked HTML samples through an allowed/manual path.

#### Discovery / Category List

| Field | Selector / Rule | Target DayDayUP field |
|---|---|---|
| Book list | `#article_list_content li` | list item root |
| Work URL | `h3 a@href` | `sourceWorkUrl`, internal only |
| Title | `h3 a@text` | `SourceWorkSearchItem.title` |
| Author | `.labelbox label:eq(0)@text` | `SourceWorkSearchItem.author` |
| Cover | `img@data-src` fallback `img@src` | `SourceWorkSearchItem.coverUrl` |
| Intro | `.ellipsis_2@text` | `SourceWorkSearchItem.intro` |
| Category/status labels | `.labelbox label:not(:first)@text`; for `/monthvisit_\d+_[12]_` reference suppresses kind | `category`, `status`, `tags` normalization inputs |
| Latest chapter | `.zxzj p@ownText` | `latestChapterName` |

#### Search Result

| Field | Selector / Rule | Target DayDayUP field |
|---|---|---|
| Search result list | `.newbox li` | result item root |
| Work URL | `h3 a@href` | `sourceWorkUrl`, internal only |
| Title | `h3 a@text` | `SourceWorkSearchItem.title` |
| Author | `.labelbox label:eq(0)@text` | `SourceWorkSearchItem.author` |
| Cover | `img@data-src` | `SourceWorkSearchItem.coverUrl` |
| Intro | `.ellipsis_2@text` | `SourceWorkSearchItem.intro` |
| Labels | `.labelbox label:not(:first)@text` | category/status/tag parsing inputs |
| Latest chapter | `.zxzj p@ownText` | `latestChapterName` |

Reference caveat: search may return Cloudflare/Turnstile challenge instead of `.newbox li`. The JSON explicitly attempts manual browser verification and cookie reuse; DayDayUP should not automate or bypass that human verification.

#### Detail

| Field | Selector / Rule | Target DayDayUP field |
|---|---|---|
| Title | `[property$=book_name]@content` | `SourceWorkDetail.title` |
| Author | `[property$=author]@content` | `SourceWorkDetail.author` |
| Cover | `[property$=image]@content` | `SourceWorkDetail.coverUrl` |
| Category | `[property$=category]@content` | `SourceWorkDetail.category` |
| Status | `[property$=status]@content` | `SourceWorkDetail.status` after mapping |
| Update time | `[property$=update_time]@content` | `SourceWorkDetail.updatedAtText` / parsed timestamp if possible |
| Latest chapter | `[property$=latest_chapter_name]@content` | `SourceWorkDetail.latestChapterName` |
| Word count | `.booknav2 p:eq(2)@text` with suffix after `|` removed | `SourceWorkDetail.wordCountText` |
| Tags | JS regex `tags:\s*'([^']*)'`, split `|` | `SourceWorkDetail.tags` |
| Description | `.navtxt p:eq(0)@textNodes` | `SourceWorkDetail.intro` |
| TOC URL | `.more-btn@href` OR `.addbtn a:eq(0)@href` | internal source TOC URL |

#### TOC

| Field | Selector / Rule | Target DayDayUP field |
|---|---|---|
| Chapter list | `#catalog li` excluding container id itself (`-id.catalog@tag.li` in reference) | `SourceTocItem[]` |
| Chapter title | `a@text` | `SourceTocItem.title` |
| Chapter URL | `a@href` | `SourceTocItem.sourceChapterUrl`, internal only |
| Order | DOM order | `SourceTocItem.ordinal` |

#### Chapter Content

| Field | Selector / Rule | Target DayDayUP field |
|---|---|---|
| Raw content | `.txtnav@textNodes` | `SourceChapterContent.rawText` |
| Cleanup | Remove footer/ad/domain markers listed below | `SourceChapterContent.sanitizedText` |

Reference cleanup regex:

```regex
\s*[（(]?本章完[）)]?\s*$|新.{0,2}书吧|吧书.{0,2}新|请记住本书首发域名.*|www\.69shuba\.com|loadAdv\([\d, ]*\);?
```

### Category, Sort, and Status Mappings

Reference discovery URLs expose numeric category and status dimensions:

| Dimension | Value | Meaning |
|---|---:|---|
| categoryCode | `0` | All categories |
| categoryCode | `1` | 玄幻魔法 |
| categoryCode | `2` | 修真武侠 |
| categoryCode | `3` | 言情小说 |
| categoryCode | `4` | 历史军事 |
| categoryCode | `5` | 游戏竞技 |
| categoryCode | `6` | 科幻空间 |
| categoryCode | `7` | 悬疑惊悚 |
| categoryCode | `8` | 同人小说 |
| categoryCode | `9` | 都市小说 |
| categoryCode | `10` | 官场职场 |
| categoryCode | `11` | 穿越时空 |
| categoryCode | `12` | 青春校园 |
| statusCode | `0` | All / unspecified |
| statusCode | `1` | 完本 / completed |
| statusCode | `2` | 连载 / ongoing |
| sort | `monthvisit` | 人气 / monthly visit ranking |
| sort | `allvote` | 推荐 / votes ranking |

DayDayUP status normalization proposal for this source contract:

| Upstream Text / Code | Standard Status |
|---|---|
| `完本`, `全本`, statusCode `1` | `completed` |
| `连载`, statusCode `2` | `ongoing` |
| empty, statusCode `0`, unknown labels | `unknown` unless detail meta confirms status |

### DayDayUP v3.0 Model Mapping

#### SourceProfile

| Field | Value / Contract |
|---|---|
| sourceKey | `69shuba` |
| displayName | `69书吧` |
| baseUrl | `https://www.69shuba.com` |
| type | text novel HTML source |
| defaultHeaders | `Referer: https://www.69shuba.com/`, `Accept-Language: zh-CN,zh;q=0.9`, plus normal browser `User-Agent` from centralized fetcher policy |
| searchSupported | `degraded`: endpoint exists, but direct search is likely Cloudflare/Turnstile blocked |
| discoverySupported | `conditional`: category/list paths exist, but currently blocked from this environment |
| charsetPolicy | search POST body `GBK`; response charset should be taken from `Content-Type`/HTML meta when available |
| antiBotPolicy | detect Cloudflare/Turnstile challenge; do not bypass; mark source diagnostic blocked/verification_required |
| publicUrlExposure | never expose upstream book/chapter/search URLs in public API responses |

#### SourceWorkSearchItem

| Standard Field | 69shuba Source |
|---|---|
| sourceId/sourceKey | configured DayDayUP source profile id / `69shuba` |
| sourceWorkKey | numeric id parsed from `/book/{id}.htm` when available |
| title | discovery/search `h3 a@text` |
| author | `.labelbox label:eq(0)@text` |
| category | normalized from labels or list URL categoryCode |
| status | normalized from labels/detail meta/statusCode |
| coverUrl | `img@data-src` or `img@src` |
| intro | `.ellipsis_2@text` |
| latestChapterName | `.zxzj p@ownText` |
| sourceUpdateText | label/detail update text when present |
| diagnostics | source elapsed, cache hit, status, result count, block reason |

#### SourceWorkDetail

| Standard Field | 69shuba Source |
|---|---|
| sourceWorkKey | numeric id from detail URL |
| title | `[property$=book_name]@content` |
| author | `[property$=author]@content` |
| coverUrl | `[property$=image]@content` |
| category | `[property$=category]@content` |
| status | `[property$=status]@content` normalized |
| updateTimeText | `[property$=update_time]@content` |
| latestChapterName | `[property$=latest_chapter_name]@content` |
| wordCountText | `.booknav2 p:eq(2)@text` before `|` |
| tags | JS `tags:'...'` split by `|` |
| intro | `.navtxt p:eq(0)@textNodes`, trim leading whitespace and preserve paragraph breaks |
| tocSourceUrl | `.more-btn@href` or `.addbtn a:eq(0)@href`, internal only |

#### SourceTocItem

| Standard Field | 69shuba Source |
|---|---|
| sourceWorkKey | parent work id |
| sourceChapterKey | chapter id parsed from chapter URL if stable |
| title | `#catalog li a@text` |
| sourceChapterUrl | `#catalog li a@href`, internal only |
| ordinal | DOM order, starting at 1 |
| volumeTitle | not observed; leave null unless TOC HTML shows grouping |
| isVip | not observed; default false/unknown |

#### SourceChapterContent

| Standard Field | 69shuba Source |
|---|---|
| sourceChapterKey | parsed from chapter URL if stable |
| title | chapter title from TOC or page title when available |
| rawText | `.txtnav@textNodes` |
| sanitizedText | raw text after source cleanup regex and DayDayUP common content sanitizer |
| contentVersion | raw, normalized, sanitized |
| sourceFetchedAt | DayDayUP fetch timestamp |
| emptyContentState | map empty `.txtnav` or all-cleaned text to `READING_CONTENT_EMPTY` |

#### diagnostics / source_time_cost

Per `.trellis/spec/backend/reading-api-contracts.md`, responses or ops views should expose source-level diagnostics. For 69shuba:

| Diagnostic Field | Contract |
|---|---|
| source_id/source_key/source_name | configured source id, `69shuba`, `69书吧` |
| elapsed_ms | measured around each HTTP fetch + parse attempt |
| cache_hit | true when DayDayUP cache served without upstream request |
| result_count | parsed item count; `0` when blocked or empty |
| status | `ok`, `blocked`, `verification_required`, `timeout`, `fetch_error`, `parse_error`, `disabled` |
| http_status | `403` observed for Cloudflare challenge |
| error_reason | safe reason such as `cloudflare_challenge`, no cookies/tokens |
| upstream_trace | safe headers such as presence of `server: cloudflare` / `cf-ray` may be logged internally; avoid public leakage if considered sensitive |

### Health and Error State Mapping

| Condition | Detection | DayDayUP State |
|---|---|---|
| Cloudflare challenge | HTTP `403` plus `Just a moment`, `challenge-platform`, `challenges.cloudflare.com`, `turnstile`, or `server: cloudflare` | `blocked` / `verification_required`; upstream error code category from reading 6xxxx contracts |
| Search verification | Search returns challenge or no `.newbox li` with challenge markers | mark search degraded; do not retry with bypass automation |
| Timeout | fetch exceeds source timeout | `timeout`; include elapsed_ms |
| Empty list | HTTP 200 with no configured list item and no challenge markers | `parse_error` or `empty` depending page type and URL |
| Empty content | `.txtnav` missing or text empty after cleanup | `READING_CONTENT_EMPTY` |
| Sanitization removes all text | non-empty raw but empty sanitized | `READING_CONTENT_SANITIZATION_FAILED` or content-empty depending existing service contract |

### Fallback Discovery Plan if Search Is Blocked

Because direct search is Cloudflare/Turnstile protected and bypass must not be attempted, DayDayUP can still model source integration around non-search paths when accessible:

1. Use category/ranking discovery URLs as the primary discovery path: `/novels/monthvisit_{categoryCode}_{statusCode}_{page}.htm`, `/novels/allvote_0_0_{page}.htm`, `/novels/male`, `/novels/female`.
2. For each discovered work, fetch detail `/book/{id}.htm` and derive canonical metadata from meta properties.
3. Derive TOC URL from detail HTML using `.more-btn@href` or `.addbtn a:eq(0)@href`; do not assume the TOC URL pattern until observed in unblocked HTML.
4. Fetch TOC and then chapter content only through DayDayUP's centralized source HTTP runtime, rate limit, timeout, SSRF guard, cache, and health reporting.
5. Mark direct source search as `verification_required` when challenge markers are returned; aggregate DayDayUP search can still search cached/materialized works.

### Actionable Parsing Contracts

| Contract | Expected Behavior |
|---|---|
| Challenge detection first | Before running CSS selectors, detect Cloudflare challenge HTML and short-circuit to blocked diagnostics. |
| Charset | For search POST encode form body with GBK; for responses rely on declared charset and fallback carefully. |
| URL canonicalization | Resolve relative URLs against `https://www.69shuba.com`; store upstream URLs internally only. |
| Work key extraction | From `/book/(\d+)\.htm`; if other detail patterns appear, add explicit extractor test. |
| Category/status | Prefer detail meta values when available; list URL category/status codes are fallback context. |
| TOC URL | Extract from detail page rather than hardcoding. |
| Chapter order | Use TOC DOM order as stable source order. |
| Content cleanup | Apply source-specific regex, then common sanitizer; preserve Chinese paragraph line breaks. |
| Public API safety | Public VOs must expose platform IDs and normalized metadata only, matching `.trellis/spec/backend/reading-api-contracts.md`. |

### Test Cases

| Test | Input / Fixture | Expected |
|---|---|---|
| Challenge homepage | Fixture with HTTP 403 and title `Just a moment...` | source diagnostic `blocked` or `verification_required`; no parser selectors executed as success. |
| Challenge search | POST `/modules/article/search.php` returns Cloudflare body | search result empty with diagnostic `verification_required`, not `ok`. |
| Search request encoding | Keyword `斗破苍穹` | form body encoded as GBK: `searchkey=<gbk bytes>&searchtype=all`. |
| Discovery URL generation | category `9`, status `2`, page `3`, sort `monthvisit` | `/novels/monthvisit_9_2_3.htm`. |
| Work key extraction | `https://www.69shuba.com/book/58687.htm` | sourceWorkKey `58687`. |
| Detail meta parse | detail fixture with `property$=book_name`, `author`, `category`, `status`, `update_time` | populated `SourceWorkDetail`; no public source URL leakage. |
| TOC parse | fixture `#catalog li a` nodes | `SourceTocItem` list with ordinal order and internal chapter URLs. |
| Content cleanup | raw text containing `本章完`, `www.69shuba.com`, `loadAdv(1, 2);` | sanitized text removes source markers while preserving story paragraphs. |
| Empty content | `.txtnav` absent or cleanup removes all text | `READING_CONTENT_EMPTY` state. |
| Diagnostics | Any blocked/timeout/success fetch | includes source id/name, elapsed_ms, cache_hit, result_count, status, reason. |

## External References

- `https://www.69shuba.com/` — direct target site; currently returns Cloudflare challenge from this environment.
- `https://www.69shuba.com/modules/article/search.php` — reference search endpoint; direct GET/POST currently returns Cloudflare challenge from this environment.
- Cloudflare challenge markers observed in response HTML (`Just a moment...`, `challenges.cloudflare.com`, `turnstile`) are used only as detection signals; no bypass was attempted.

## Related Specs

- `.trellis/spec/backend/reading-api-contracts.md` — v3.0 redesign direction requires DayDayUP-owned source profiles/parsers, per-source diagnostics, source failure isolation, and no public source URL leakage.

## Caveats / Not Found

- All direct public page fetches from this environment were blocked by Cloudflare HTTP `403`; therefore homepage/list/detail/TOC/chapter/search business HTML could not be directly observed in this run.
- Selectors listed above are from `docs/书源/69shuba.json` and must be validated against captured unblocked HTML before being treated as production-stable.
- Search is explicitly documented in the reference JSON as requiring Cloudflare Turnstile/human verification in some cases. This research did not attempt to bypass or automate verification.
- TOC and chapter URL examples are candidates based on tested paths and reference selector shape; actual URLs should be extracted from unblocked detail/TOC pages.
- No cookies, tokens, or verification artifacts were stored.
