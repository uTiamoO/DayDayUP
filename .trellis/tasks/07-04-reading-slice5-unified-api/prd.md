# 阅读中台切片5：聚合搜索与统一阅读 API

## Goal

在已完成书源导入/编译、指定书源运行时、作品/章节资产化、正文抓取与净化的基础上，完成切片 5 的用户侧闭环：提供 `/api/v1/reading` 统一阅读 API，支持聚合/指定书源搜索、作品列表、作品详情、章节列表、正文读取，并补齐网关 `/reading` 转发配置。

## Requirements

- 聚合搜索必须支持 `mode=aggregate|source`：
  - `source` 模式要求传入 `sourceId`，只基于指定书源搜索并将候选资产化。
  - `aggregate` 模式面向统一作品视图，返回平台已沉淀作品，并在需要时可基于可用书源进行内容发现。
- 搜索结果必须返回统一作品候选、命中来源数、聚合状态与分页信息；不得向调用方暴露源站明文 URL 作为后续 API 入参。
- 作品列表、作品详情、作品来源列表必须基于 `Work` / `WorkSourceBinding` 资产模型输出。
- 章节列表必须基于统一章节资产输出；当调用方指定来源且该来源章节绑定已存在时，返回来源可用性信息。
- 正文接口必须以 `chapterId + sourceId` 为入口，默认返回 `sanitized` 正文；仅当请求 `raw` 或 `normalized` 时返回对应内部版本。
- 正文接口默认 `fetchPolicy=cache-first`；`force-refresh` 时允许触发回源抓取与净化。若缺少可用来源或正文为空，应复用阅读域 6xxxx 错误码。
- 所有接口返回统一 `R<T>`；分页返回 `{ list, total, page, pageSize, hasNext }`。
- 网关新增 `/reading/**` 路由到阅读服务并 StripPrefix；`/api/v1/internal/**` 不新增对外网关路由。
- 第一版不做全局跨源章节对齐、不做复杂搜索引擎、不做公开鉴权策略变更。

## Acceptance Criteria

- [ ] 新增用户侧 Controller，暴露 `GET /api/v1/reading/search`、`GET /api/v1/reading/works`、`GET /api/v1/reading/works/{workId}`、`GET /api/v1/reading/works/{workId}/sources`、`GET /api/v1/reading/works/{workId}/chapters`、`GET /api/v1/reading/chapters/{chapterId}/content`。
- [ ] `mode=source` 搜索可复用指定书源链路完成搜索与资产化，返回统一作品分页结构。
- [ ] `mode=aggregate` 搜索返回统一作品聚合视图，包含来源数量与 `aggregationStatus`。
- [ ] 作品列表/详情/来源/章节接口不暴露真实源站 URL，调用方只使用平台内部 ID。
- [ ] 正文接口默认返回 `sanitized`；在无净化结果但允许 cache-first 回源时，能抓取、净化并返回可读正文。
- [ ] 新增或调整 Mapper/Service 单测覆盖搜索、列表、章节、正文默认版本与强制刷新路径。
- [ ] 网关配置包含 `/reading/**` StripPrefix 路由，且未对 `/api/v1/internal/**` 建立外部路由。
- [ ] Maven 验证通过：`mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test`（使用本机 JDK 21）。

## Notes

- 权威需求来源：`PRD/novel_aggregator_prd.md` §9/§10/§12，以及 `docs/superpowers/specs/2026-07-02-reading-api-platform-design.md` §6.2/§10.6。
- 本切片为复杂任务，需配套 `design.md` 与 `implement.md` 后再 start。
