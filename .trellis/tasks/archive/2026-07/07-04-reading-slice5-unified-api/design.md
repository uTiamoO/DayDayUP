# Design: 阅读中台切片5：聚合搜索与统一阅读 API

## Scope

本设计只覆盖切片 5：把已沉淀的阅读资产与指定书源运行链路包装为稳定用户侧 API，并补齐网关路由。书源编译、运行时抓取、作品/章节资产化、正文抓取与净化沿用既有服务。

## API Boundary

新增 `com.yuan.daydayup.reading.api.controller` 或 `reading.api` 包下的用户侧 Controller，路径前缀：`/api/v1/reading`。

接口：

- `GET /search`
  - 参数：`keyword,page,pageSize,mode,sourceId,category,completionStatus`
  - `mode=source`：校验 `sourceId`，调用 `ContentDiscoveryService.discover(sourceId, keyword, page)`，将入库结果转换成分页 VO。
  - `mode=aggregate`：查询 `reading_work` 聚合资产；若 keyword 非空，按标题/作者模糊匹配，返回已沉淀作品视图。
- `GET /works`
  - 参数：`category,status,sort,sourceId,page,pageSize`
  - 查询统一作品列表，可按绑定来源过滤。
- `GET /works/{workId}`
  - 返回作品详情、来源数量、主来源摘要、最新章节摘要。
- `GET /works/{workId}/sources`
  - 返回可用来源列表，只暴露 sourceId/sourceName/priority/status/isPrimarySource 等平台字段，不暴露 sourceBookUrl。
- `GET /works/{workId}/chapters`
  - 参数：`sourceId,page,pageSize,refreshPolicy`
  - 默认读库；`force-refresh` 且提供 sourceId 时先调用 `ChapterSyncService.syncToc(workId, sourceId)`。
- `GET /chapters/{chapterId}/content`
  - 参数：`sourceId,contentVersion=latest|raw|normalized|sanitized,fetchPolicy=cache-first|force-refresh`
  - 默认 latest 等价 sanitized。cache-first 优先读 `ChapterContentSnapshot.sanitizedContent`；缺失时调用 `ContentFetchService.fetchAndStore` 后调用 `ContentSanitizeService.sanitize`。

## Service Boundary

新增用户侧查询编排服务，建议命名：

- `ReadingQueryService`
  - 负责搜索、作品列表、作品详情、来源列表、章节列表。
  - 依赖 `WorkMapper`、`WorkSourceBindingMapper`、`ChapterMapper`、`ChapterSourceBindingMapper`、`ContentDiscoveryService`、`ChapterSyncService`。
- `ReadingContentService`
  - 负责正文版本选择与 cache-first / force-refresh 编排。
  - 依赖 `ChapterContentSnapshotMapper`、`ContentFetchService`、`ContentSanitizeService`。

保留现有 ops/service 职责：ops 仍用于内网治理；用户侧服务不直接暴露治理能力。

## Data Access Changes

现有 Mapper 只覆盖前几片的写入和定点查询，需要补齐读侧查询：

- `WorkMapper`
  - 分页搜索统一作品：keyword 可匹配 title/author，支持 category/status。
  - 按 sourceId 过滤作品（join binding 或 exists）。
- `WorkSourceBindingMapper`
  - 查询 workId 下所有来源绑定。
  - 统计 workId 的来源数量。
- `ChapterMapper`
  - 分页查询 workId 下统一章节，按 chapterIndex 升序。
  - 查询 workId 最新章节。
- `ChapterSourceBindingMapper`
  - 查询章节在指定来源下的绑定可用性。
- `ChapterContentSnapshotMapper`
  - 已有 `selectByChapterAndSource` 可复用；如缺少 sanitized/raw 字段映射，补齐 VO 转换。

若 MyBatis 注解 SQL 变复杂，优先保持与现有风格一致；必要时再引入 XML。

## VO Contract

在 `daydayup-reading-api` 新增用户侧 VO：

- `ReadingPageVO<T>`：`list,total,page,pageSize,hasNext`。
- `ReadingWorkVO`：作品摘要 + `sourceCount` + `aggregationStatus`。
- `ReadingWorkDetailVO`：作品详情 + source summary + latest chapter。
- `ReadingSourceVO`：平台来源摘要，不含源站 URL。
- `ReadingChapterVO`：章节摘要 + source availability。
- `ReadingContentVO`：chapterId/sourceId/contentVersion/content/contentStatus/qualityScore/sanitizationRunId/freshlyFetched。

如项目已有通用分页 VO，可复用；否则在 reading-api 内定义轻量 VO。

## Content Version Rules

- `latest` / `sanitized`：返回 `ChapterContentSnapshot.sanitizedContent`。若为空：
  - `cache-first`：允许同步补抓 + 净化一次；若仍无 sanitized，按净化状态返回业务错误。
  - `force-refresh`：强制抓取 + 净化后返回。
- `normalized`：返回 normalizedContent；缺失时可触发 fetchAndStore。
- `raw`：返回 rawContent；缺失时可触发 fetchAndStore。
- `rejected` 净化不得覆盖旧 sanitized，沿用切片 4 规则。

## Gateway

在 gateway 配置中新增 reading 路由：

- Path：`/reading/**`
- StripPrefix：1
- URI：阅读服务 lb 名称（按现有服务命名约定）

不新增 `/api/v1/internal/**` 路由。

## Errors

- 参数非法：`READING_INVALID_ARGUMENT`。
- mode 不支持：`READING_UNSUPPORTED_MODE`。
- 作品/章节不存在：`READING_WORK_NOT_FOUND` / `READING_CHAPTER_NOT_FOUND`。
- 无可用来源：`READING_SOURCE_NOT_AVAILABLE`。
- 抓取、超时、SSRF、净化错误沿用下游服务抛出的 6xxxx 错误码。

## Validation

- 单测覆盖 service 编排，不依赖真实网络。
- Controller 层可用 Spring MVC 测试或轻量服务测试覆盖参数默认值。
- 最终运行 reading-biz Maven test。
