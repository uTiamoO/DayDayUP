# 阅读聚合服务 产品需求文档 (PRD)

> **本文件定位**：本 PRD 是「高性能小说聚合搜索与阅读平台」的**产品需求层**（WHAT / WHY + 对外契约 + 验收标准）。
> **路线重定稿（2026-07-09）**：平台不再以“导入开源阅读书源并逐条解析执行”为主线；开源阅读/Legado 书源只作为站点覆盖、字段命名、API 入口和 URL 模板的参考素材。具体解析、拆解、合并、广告移除、正文净化、质量评估、缓存与高可用治理均由 DayDayUP 平台自行设计和实现。
> **技术设计以下列规格为权威来源（HOW）**，本文不重复设计细节；既有 reading spec 中与 v3.0 路线冲突的“直接导入并执行外部书源/RuleModel”内容需在后续规格重构中废弃或降级为迁移参考：
> - `docs/superpowers/specs/2026-07-02-reading-api-platform-design.md`（历史架构参考，待重构）
> - `docs/superpowers/specs/2026-07-02-reading-rulemodel-v1.md`（历史 RuleModel 参考，v3.0 不作为主线）
> - `docs/superpowers/specs/2026-07-02-reading-js-engine-decision.md`（历史运行时参考）
> - `docs/superpowers/specs/2026-07-02-reading-source-rule-inventory.md`（书源生态普查参考）
>
> 版本：v3.0（竞品对标与平台路线重定稿）  |  最后更新：2026-07-09  |  状态：草案 / 待评审

## 修订记录

| 版本 | 日期 | 变更摘要 |
| ---- | ---- | -------- |
| v1.0 | 2026-07-02 | 初稿：`daydayup-novel` 独立聚合服务，直接解析 Legado + 缓存，单表 + 3 API |
| v1.1 | 2026-07-03 | 对齐工程约定（R/BaseEntity/错误码），补安全/NFR/AC；标注书源格式范围 |
| **v2.0** | 2026-07-03 | **确认与「阅读中台」为同一系统；本文重定位为中台产品需求层，全面对齐 reading 命名与架构（编译 RuleModel、全量持久化、8 实体、GraalJS）；技术设计移交 5 份 spec** |
| **v3.0** | 2026-07-09 | **根据“大灰狼聚合”竞品考察与当前用户目标重新规划：定位升级为高性能小说聚合搜索/阅读平台；开源阅读书源仅作参考，不再把直接导入执行 Legado/RuleModel 作为主线；新增多源编排、`source_time_cost` 类可观测、会员密钥、正文访问限流、规则包分发、轻量用户中心、服务/书源健康等产品目标** |

> **重要**：v1.x 曾设计为独立的 `daydayup-novel` 服务（直接解析 + 缓存 + 单表）。经确认与阅读中台是**同一个系统**，该路线作废。命名统一为 `daydayup-reading`，架构改为「编译式模块化单体」，见 §3。

## 术语表

| 术语 | 说明 |
| ---- | ---- |
| 开源阅读书源参考 | Legado/阅读生态中的站点覆盖、字段命名、API 入口、URL 模板、客户端导入协议等参考素材；v3.0 不再把外部书源作为可直接执行的生产规则 |
| 自研源配置 / SourceProfile | DayDayUP 平台自定义的来源配置资产，描述来源身份、入口、限速、健康、解析策略引用等，不等同于 Legado 书源原文 |
| 自研解析与净化 | 平台自行完成页面/API 请求、字段抽取、作品/章节合并、正文标准化、广告移除、质量评估与发布 |
| Work / Chapter | 平台内部统一作品 / 统一章节（与来源书籍/章节解耦，多源可绑定同一 Work） |
| 聚合编排 | 面向多个来源并发/降级搜索，合并结果并输出 per-source 耗时、缓存命中、禁用源等观测信息 |
| 会员密钥 / user_key | 面向阅读客户端或用户的访问密钥，后续与正文访问限流、在线 IP/设备、阅读统计、封禁审计形成闭环 |
| raw / normalized / sanitized | 正文三层：原始 / 标准化 / 净化，物理持久化 raw+sanitized |

---

## 1. 项目概述

### 1.1 目标与定位
在现有 Java / Spring 多模块工程内，重新规划并演进一个**高效、高性能、高可用的小说聚合搜索与阅读平台**：以多个内容来源为供给，参考开源阅读生态的站点覆盖、API 入口和客户端导入习惯，但平台不直接依赖外部书源规则作为生产执行模型；由 DayDayUP 自行完成来源配置、聚合编排、解析抽取、作品/章节合并、正文净化、广告移除、质量评估、缓存、限速、熔断、健康观测和访问治理。

核心价值：把**来源变成可治理的平台资产**、**聚合变成可观测的编排能力**、**正文变成可回放/可净化/可演进的内容资产**、**访问变成可限流/可审计/可防共享的用户闭环**，而不是把外部书源规则作为一次性爬虫脚本执行。

竞品“大灰狼聚合”的启发已沉淀到 `PRD/dahuilang_competitor_analysis.md`：多源聚合编排与 per-source 可观测优先；会员密钥与正文访问限流紧随其后；规则包分发、轻量用户中心、服务/书源健康作为产品化演进。

### 1.2 第一期范围（v3.0 重定稿）
**第一期做**：
- 小说文字内容的聚合搜索、详情、目录、正文 API；支持聚合模式与指定来源模式。
- 自研来源配置与解析策略：参考开源阅读书源/API 形态，但解析、拆解、合并、广告移除、正文净化由平台实现。
- 多源聚合编排：并发请求多个来源，单源失败隔离，输出 `source_time_cost` 类 per-source 耗时、缓存命中、结果数量、禁用源等观测信息。
- 统一作品 / 章节 / 正文资产模型；保守归并、人工纠错、raw / normalized / sanitized 三层内容资产。
- 高性能与高可用基础：热门搜索/目录/正文缓存、per-source 限速、超时降级、熔断、后台补抓/重处理任务。
- 基础运营能力：来源管理、来源健康、聚合耗时与成功率、正文质量、任务状态。
- 为 P1 会员密钥与正文访问限流预留产品契约：正文访问是后续在线状态、IP/设备限制、阅读统计、封禁审计的主控制点。

**第一期不做**：直接导入并执行开源阅读书源作为生产主线、复杂权限中心、公开商业化门户、客户端下载站、EPUB/下载能力、漫画/听书/短剧正文能力、全站扫描、重型分布式/MQ 架构。

### 1.3 开源阅读生态参考边界
- 开源阅读/Legado 书源可作为**参考资料**：站点清单、字段命名、搜索/详情/目录/正文入口、URL 模板、header/cookie 形态、客户端导入协议。
- 平台不得把未审计的外部规则/脚本作为生产执行主线；需要沉淀为 DayDayUP 自研源配置、解析策略、净化规则和质量标准。
- 既有 RuleModel / GraalJS / 书源编译成果作为历史技术资产保留，可用于迁移分析、样例回归或内部工具，但 v3.0 后续切片应围绕自研聚合平台重构规格。
- 第一阶段仍只覆盖 `bookSourceType=0` 对应的文字小说能力；漫画、听书、短剧等多内容形态作为 P3 远期扩展。

### 1.4 合规与免责（对齐主规格 §12.3）
- 抓取内容对外分发存在**根本性版权风险**；第一期定位**个人 / 内网自用**，架构不为公开分发过度设计。
- 公开化（阶段 3）**前置条件**：完成内容版权与合规评估，结论为准。
- 正文持久化与对外接口提供配置开关，可整体停用。前端须展示来源与免责声明。

---

## 2. 用户与使用场景

| 角色 | 场景 | 对应 API 层 |
| ---- | ---- | ---- |
| 阅读客户端 / 前端 | 搜索书 → 看详情 → 看目录 → 读正文（净化后）；未来可携带会员密钥访问正文 | 统一阅读 API `/api/v1/reading` |
| 聚合调用方 | 需要跨多个来源获得候选结果，并看到每个来源耗时、缓存命中、禁用原因 | 聚合搜索 API + per-source 观测字段 |
| 运维 / 开发（调试灰度） | 指定来源定向抓取、看运行诊断、验证解析策略 | 来源定向 API `/api/v1/internal/source-reading` |
| 运营 / 治理 | 管理来源、查看健康、调整禁用源、重跑净化、归并纠错、查看访问统计 | 运营 API `/api/v1/internal/ops` |
| 用户中心（P2） | 查看密钥、会员状态、阅读统计、在线 IP/设备、封禁状态 | 轻量用户中心 API |
| 其它微服务 | 内部聚合调用 | `daydayup-reading-api` Feign facade |

---

## 3. 架构与工程接入（v3.0 产品边界，HOW 待规格重构）

- **架构定位**：高性能小说聚合搜索与阅读平台，第一期仍可采用模块化单体部署，但内部边界围绕来源中心 `source` / 聚合编排 `orchestrator` / 自研解析 `parser` / 内容净化 `pipeline` / 内容资产 `repository` / 阅读 API `api` / 运营与健康 `ops` / 访问治理 `access` / 任务 `task` 拆分。
- **规则路线**：外部开源阅读书源不作为生产直接执行规则；需转化/沉淀为 DayDayUP 自研源配置、解析策略和净化规则。既有 RuleModel/GraalJS 能力作为历史资产和迁移参考，不再定义新路线主干。
- **聚合路线**：搜索/详情/目录/正文按来源并发编排、限速、超时、熔断和降级；单源故障不得拖垮整体结果；API 与运营侧必须能看到 per-source 成功率、耗时、缓存命中、结果数量、禁用原因。
- **模块**（`daydayup-modules` 下，与 game/social 并列）：
  - `daydayup-reading-api`：对外 DTO、API 契约、OpenAPI 模型、内部 facade。
  - `daydayup-reading-biz`：上述 package 的实现，含 `ReadingApplication`。
- **数据库**：一服务一库 `daydayup_reading`；entity/mapper 包结构沿用 auth/admin 约定；主键雪花，基础字段对齐 `BaseEntity`（`create_time/update_time/create_by/update_by/deleted`）。后续模型需覆盖来源配置、来源健康、聚合调用记录、作品/章节/正文资产、会员密钥、正文访问会话、封禁审计。
- **网关**：统一阅读 API 经网关 `/reading` 前缀转发（StripPrefix，对照 admin）；`/api/v1/internal/**` **不配置对外路由**（仅内网可达，网关层显式排除，不靠命名约定）。
- **配置**：抓取敏感项（代理 / 凭据）走 `application-local.yml`（gitignore）+ Nacos dev namespace，启动 `--spring.profiles.active=local`。

> 数据模型与解析/净化/聚合实现细节需在 v3.0 后续 design/spec 中重写；本文只定义产品目标与分期边界。

---

## 4. 对外 API 契约（产品级，对齐主规格 §6）

### 4.0 通用约定
- **统一返回体** `R<T>`：`{ "code": 200, "message": "操作成功", "data": <T>, "timestamp": <ms> }`。
- **分页**：`data: { list, total, page, pageSize, hasNext }`。
- **鉴权**：第一期内网自用，统一阅读 API 可暂不接鉴权，但网关层保留接入平台 JWT 的扩展位；运营 / 定向 API 仅内网可达。
- **API 版本**：路径带 `/api/v1`，预留版本治理。

### 4.1 统一阅读 API `/api/v1/reading`（对外稳定门面）
| 方法 | 路径 | 说明 | 关键参数 |
| ---- | ---- | ---- | ---- |
| GET | `/search` | 搜索作品（聚合 / 指定源） | `keyword,page,pageSize,category,completionStatus,mode=aggregate\|source,sourceId` |
| GET | `/works` | 作品列表 | `category,status,sort,sourceId,page,pageSize` |
| GET | `/works/{workId}` | 作品详情（可用来源摘要） | `sourceId,refreshPolicy=cache-first\|force-refresh` |
| GET | `/works/{workId}/sources` | 作品可用来源（可选） | — |
| GET | `/works/{workId}/chapters` | 章节列表 | `sourceId,page,pageSize,refreshPolicy=cache-first\|force-refresh` |
| GET | `/chapters/{chapterId}/content` | 章节正文 | `sourceId,contentVersion=latest\|raw\|normalized\|sanitized,fetchPolicy=cache-first\|force-refresh` |

- 搜索返回：作品候选列表 + 聚合摘要 + 命中来源数 + 分页。**聚合模式返回候选聚合结果，不强行声明唯一真相**（保守归并，见 §5）。
- 聚合搜索响应需要面向调用方或运营侧提供 `source_time_cost` 类观测信息：每个来源的耗时、缓存命中、结果数量、状态/禁用原因；单源失败以降级信息体现，不应导致整体搜索不可用。
- 详情：默认 `cache-first`，完整缓存直接返回；缓存不完整时自动从主来源回源。`force-refresh` 可刷新指定来源，未指定时使用主来源；只有主来源可更新统一作品元数据，副来源仅更新自身绑定信息。
- 目录：默认 `cache-first`，但空目录或指定来源没有章节绑定时会自动同步；`force-refresh` 仍要求 `sourceId` 并强制同步该来源。
- 正文：**Public 默认返回 `sanitized`**；`cache-first` 在请求版本已缓存时直接返回，缺失必要内容时才回源，已有 raw/normalized 时优先复用缓存完成净化；`force-refresh` 强制重新抓取指定来源。同步回源整链路上限 ~10s，超时返回 `UPSTREAM_TIMEOUT` 并落补抓任务，客户端可稍后重查。
- P1 会员密钥接入后，正文 API 是访问治理主控制点：读取正文时记录在线状态、IP/设备、阅读次数，并执行密钥等级对应的并发/IP 限制。

### 4.2 书源定向 API `/api/v1/internal/source-reading`（内网，调试 / 灰度）
指定 `sourceId` 的 search / detail / toc / content + 运行诊断。

### 4.3 运营与治理 API `/api/v1/internal/ops`（内网）
管理来源配置、查看来源健康、触发同步、重跑净化、健康巡检、手动回源重抓、查看聚合调用诊断、管理禁用源、**作品归并纠错**（解绑 / 重绑 / 拆分作品，同步迁移章节绑定）。

### 4.4 规则包/源配置分发 API（P1/P2）
面向阅读客户端或内部调用方分发 DayDayUP 自研源配置包/规则包，提供版本号、更新日志、兼容客户端、普通/VIP 可见性、导入协议链接等产品字段。该能力参考竞品书源分发生态，但不等同于直接输出未审计外部书源规则。

### 4.5 轻量用户中心与健康状态 API（P2）
面向自用/运营场景提供用户密钥、会员状态、阅读统计、在线 IP/设备、封禁状态、服务健康、来源健康、禁用源列表等查询能力。第一期只预留契约，不建设复杂权限中心或公开商业化门户。

> **对比 v1.x**：原稿以源站明文 `tocUrl/contentUrl` 直传（SSRF 隐患）已废弃；改为 `workId/chapterId + sourceId` 的平台内部主键，真实源站 URL 只在运行时内部持有，从 API 契约层根除 SSRF 暴露面。

---

## 5. 聚合搜索需求（"aggregator" 核心，v3.0 重定稿）

- **两种模式并存**：`aggregate`（跨源归并候选）与 `source`（指定来源直取）。
- **平台自研编排**：聚合层负责选择来源、并发调度、限速、超时、熔断、降级、缓存读取、结果合并和诊断输出；不得把外部书源脚本作为不可治理黑盒。
- **per-source 可观测**：搜索/详情/目录/正文关键链路都应记录来源维度耗时、缓存命中、结果数量、错误类型、禁用原因，搜索响应或运营诊断中提供 `source_time_cost` 类字段。
- **保守归并**：第一版按「标题 + 作者 + 站点信息」保守匹配；允许多源命中同一本书；`aggregationStatus ∈ {single_source, merged, suspect}`，`suspect` 待人工确认。
- **可纠错**：误归并通过运营 API 解绑 / 重绑 / 拆分修复（同步迁移章节绑定）。
- **限速共享**：聚合调用与后台批量任务**共用** per-source 限速器（每源并发上限 + 最小请求间隔），所有出站请求都必须走统一 HTTP 出口，不得绕行。
- **排序 / 去重**（产品要求，编排/归并层实现）：候选排序综合来源优先级、关键词与标题相似度、字段完整度、来源健康、缓存新鲜度；同一 Work 下多源按可用性与优先级择优。
- **高可用降级**：单源失败、超时或被禁用时，聚合结果应保留其它来源结果，并在诊断字段中明确失败来源与原因。

> 说明：具体相似度算法、缓存 key、并发模型、熔断阈值属实现细节，归入 v3.0 design/spec，不在 PRD 硬编码。

---

## 6. 错误模型（复用 R + 阅读域错误码段）

复用统一 `R` 与错误码分段规范。阅读域分配 **6xxxx 段**（已确认与既有规划不冲突），映射主规格 §6.3 语义分类，待补入 `ErrorCode` 枚举：

| Code | 常量名 | 语义分类 | 含义 |
| ---- | ---- | ---- | ---- |
| 60001 | READING_WORK_NOT_FOUND | 业务 | 作品不存在 |
| 60002 | READING_CHAPTER_NOT_FOUND | 业务 | 章节不存在 |
| 60003 | READING_SOURCE_NOT_AVAILABLE | 业务 | 无可用书源 |
| 60101 | READING_UPSTREAM_FETCH_FAILED | 抓取 | 源站抓取失败 |
| 60102 | READING_UPSTREAM_TIMEOUT | 抓取 | 源站抓取超时（已落补抓任务） |
| 60103 | READING_UPSTREAM_BLOCKED | 抓取 | 目标被安全策略拦截（SSRF 防护 / 私网 / webView） |
| 60201 | READING_SOURCE_PROFILE_INVALID | 来源配置 | 自研来源配置/解析策略校验失败 |
| 60202 | READING_PARSER_RUNTIME_FAILED | 解析 | 自研解析运行失败 |
| 60301 | READING_CONTENT_EMPTY | 内容 | 正文为空 |
| 60302 | READING_CONTENT_SANITIZATION_FAILED | 内容 | 净化失败（抓取可能成功） |
| 60303 | READING_CONTENT_QUALITY_LOW | 内容 | 正文质量过低 |
| 60401 | READING_INVALID_ARGUMENT | 请求 | 参数非法 |
| 60402 | READING_UNSUPPORTED_MODE | 请求 | 不支持的调用模式 |

---

## 7. 安全需求

- **SSRF**：对外契约不接收源站明文 URL（§4.3）；运行时出站统一经校验——目标 host 必须落在受信任来源配置范围内，解析 IP 禁私网 / 回环 / 链路本地，仅 http/https，重定向后再校验；命中拦截返回 `60103`。
- **外部规则安全**：开源阅读/Legado 书源只作为参考素材；未经审计的外部脚本、webView 依赖、动态代码不得作为生产主线直接执行。
- **凭据 / 健康**：来源静态 header 或站点凭据标记并纳入健康巡检；到期即整源失效，必要时通过受控探活刷新状态。
- **会员密钥与防共享（P1）**：`user_key` 等访问密钥不得明文泄露；正文访问链路记录在线状态、IP/设备、阅读次数和封禁审计，普通/VIP/SVIP 等等级的并发/IP 限制在产品化切片中落地。
- **接口暴露**：`/api/v1/internal/**` 网关不配对外路由。

---

## 8. 非功能需求 (NFR)

| 维度 | 目标 |
| ---- | ---- |
| 性能 | 命中资产库正文 P95 ≤ 300ms；回源正文（抓取+净化）整链路上限 ~10s，超时降级补抓 |
| 并发 / 限速 | per-source 并发上限 + 最小请求间隔，聚合与后台任务共用限速器；外部请求线程池与 Web 隔离 |
| 可用性 | 单源故障不影响整体；编排层按来源隔离超时/熔断/禁用；服务可水平扩缩 |
| 一致性 / 可回放 | raw + sanitized 持久化；书源版本、编译版本、净化规则版本、正文版本可追溯回放 |
| 可观测 | 接入 `daydayup-common-log`；埋点来源成功率 / 耗时 / 缓存命中 / 禁用原因 / 结果数量 / 正文质量，API 或运营诊断暴露 `source_time_cost` 类指标 |
| 安全 | §7 全部满足 |
| 合规 | 正文持久化与对外接口可整体开关（§1.4） |

---

## 9. 验收标准 (Acceptance Criteria，对齐主规格 §11 成功标准)

- [ ] `daydayup-reading-api` / `daydayup-reading-biz` 建成，注册进 Nacos，网关 `/reading` 可路由，`/api/v1/internal/**` 确认无对外路由。
- [ ] **来源资产化**：开源阅读生态资料可被整理为 DayDayUP 自研源配置/解析策略；来源状态、健康、限速、禁用原因可见。
- [ ] **指定书源链路**：篱笆文学（HTML）与猫眼看书（JSON + crypto）两个固定回归样例，稳定完成搜索 / 详情 / 目录 / 正文。
- [ ] **正文净化可见效**：广告移除、尾巴清理、敏感词修复生效，且净化结果（trace / 质量分 / 规则版本）有记录。
- [ ] **统一 API 可消费**：调用方不关心书源细节即可完成 search / works / detail / chapters / content；Public 默认返回 sanitized。
- [ ] **内容资产可回放**：可查看 raw / normalized / sanitized / 净化记录 / 规则版本 / 来源版本。
- [ ] **聚合与纠错**：聚合搜索返回候选、命中来源数、`source_time_cost` 类 per-source 耗时/缓存/结果数/禁用源诊断；误归并可经运营 API 解绑 / 重绑 / 拆分修复。
- [ ] **安全**：伪造私网 / webView 目标被拦截返回 `60103`；自研解析策略或受控脚本执行有超时/资源上限保护，不钉住线程。
- [ ] **限速**：统一 API 与后台批量抓取均经 per-source 限速器，无绕行。
- [ ] **会员密钥与正文访问限流（P1）**：正文访问可纳入 `user_key`、会员等级、在线 IP/设备、阅读次数、封禁审计闭环。
- [ ] **规则包/源配置分发（P1/P2）**：可分发 DayDayUP 自研规则包/源配置，包含版本、更新日志、兼容客户端和导入入口。
- [ ] **轻量用户中心与健康状态（P2）**：可展示用户密钥/会员/阅读统计/在线设备，以及服务健康、来源健康和禁用源状态。

---

## 10. 里程碑 / 交付切片（v3.0 推荐路线，按可交付闭环）

| 切片 | 目标 | 关键交付 |
| ---- | ---- | ---- |
| **切片 0**（已完成，历史资产） | 旧路线可行性验证 | 覆盖率普查 68%、JS 引擎选定 GraalJS、双加密变体端到端跑通；作为 v3.0 参考，不再作为主线约束 |
| **切片 A：路线重定稿** | 自研聚合平台方案落地 | 更新 PRD/spec/design；明确外部书源仅参考、SourceProfile/Parser/Orchestrator/Access/Health 等新边界 |
| **切片 B：多源聚合编排与 per-source 可观测（P0/P1）** | 搜索聚合高可用闭环 | 来源选择、并发编排、限速/超时/熔断、`source_time_cost`、禁用源、聚合诊断 API |
| **切片 C：自研解析与内容资产化（P1）** | 详情/目录/正文可持续生产 | 自研解析策略、作品/章节合并、raw/normalized/sanitized 三层正文、广告移除、质量评估 |
| **切片 D：高性能缓存与任务补偿（P1）** | 高性能与高可用 | 热门搜索/目录/正文缓存、异步补抓、重处理任务、单源故障隔离、降级返回 |
| **切片 E：会员密钥与正文访问限流（P1）** | 防共享与访问治理闭环 | `user_key` 生命周期、会员等级、正文访问会话、在线 IP/设备、阅读统计、封禁审计 |
| **切片 F：规则包/源配置分发中心（P1/P2）** | 客户端导入与版本运营 | 自研规则包/源配置版本、更新日志、普通/VIP 可见性、兼容客户端、导入协议链接 |
| **切片 G：轻量用户中心与服务/书源健康（P2）** | 可运营可自助 | 用户密钥/会员/阅读统计/在线设备页，服务健康、来源健康、禁用源状态页 |
| **切片 H：远期扩展（P3）** | 非第一期能力 | 多内容形态（漫画/听书/短剧）、客户端下载、EPUB/下载等 |

---

## 11. 开放问题 (Open Questions)

| # | 问题 | 状态 |
| ---- | ---- | ---- |
| O1 | 与「阅读中台」的关系 | **已定**：同一系统，命名继续使用 `daydayup-reading`；v3.0 路线重定稿为小说聚合搜索与阅读平台 |
| O3 | 阅读域错误码段位 | **已定**：6xxxx，不冲突；✅ 已补入 `ErrorCode` 枚举（60001~60402） |
| O6 | 文件与模块归位 | 建议将本文件更名为 `reading_platform_prd.md` 并入 reading 轨；模块 `daydayup-reading-*`。待确认后可执行 git mv |
| O7 | 开源阅读/Legado 书源定位 | **已定**：仅作为参考素材和迁移分析输入，不再作为生产直接执行主线 |
| O5 | 正文缓存 / 持久化默认策略与版权 | 默认开关与 TTL 需产品 / 法务拍板；公开化前置版权评估 |
| O8 | v3.0 技术规格重构 | **待办**：重写 SourceProfile、Parser、Orchestrator、AccessLimit、Health、Distribution 等规格，替代旧 RuleModel-first 主线 |
| O9 | 逻辑删除与书源唯一键交互 | ✅ **已解决**：导入查询绕逻辑删除（`selectByUrlIncludeDeleted`），软删同 URL 重导入走「恢复」语义（deleted 置回 0，status/priority 重置为书源自身值）；后续需迁移为来源配置唯一键策略 |
| O10 | 内网 ops 端点鉴权 | ✅ **已解决**：`ReadingSecurityConfig` 覆盖 common-security 默认 chain，放行 `/api/v1/internal/**`（网关保证不对外路由） |

---

## 12. 实现进度（随切片推进更新）

| 切片 · 步骤 | 状态 | 交付 |
| ---- | ---- | ---- |
| 切片 0 | ✅ 已闭环 | 覆盖率普查 68%、GraalJS 选型、双加密变体验证（见 specs） |
| 切片 1 · 步骤 1：M0 骨架 + 书源导入闭环 | ✅ 已完成（`mvn test-compile` 通过） | `daydayup-reading-{api,biz}` 模块、`daydayup_reading` 库 + `reading_source_definition` 表、`SourceDefinition` 实体/Mapper、`SourceImportService`（按 `bookSourceUrl` 幂等 upsert + SHA-256 指纹 + 保留 status/priority）、`POST/GET /api/v1/internal/ops/sources/import\|count` |
| 切片 1 · 步骤 2：RuleModel 自研解析器 + 编译体检 | ✅ 已完成（`mvn test` 通过，13 测试全绿） | RuleModel 领域模型、`RuleStringParser`（§7 tokenizer：jsoup 方言/jsonpath/模板/组合器/##）、`NativeConverter`（crypto→原生 postProcessor）、`RuleCompiler`（三级体检 full/degraded/rejected）、`SourceCompiledRule` 表 + `RuleCompileService`、`POST /ops/sources/{id}/compile\|compile-all`、`ReadingSecurityConfig`（解决 O10） |
| 切片 2 · 子片 1：离线抽取执行引擎 | ✅ 已完成（`mvn test` 通过，reading 21 测试全绿） | `RuleExecutor` + `HtmlExtractor`（jsoup Legado 方言）+ `JsonExtractor`（jayway）+ `TemplateRenderer` + `NativePostProcessors`（aes/base64/hex/md5）；端到端验证篱笆 HTML / 猫眼 JSON + aes 密文全链路 |
| 切片 2 · 子片 2a：网络出站 + SSRF + 限速 + 定向 API | ✅ 已完成（`mvn test` 通过，reading 48 测试全绿） | 编译器装配 `RequestSpec/Http`（searchUrl 选项段/宽松 header/concurrentRate，编译器 v1.1）、`HttpFetcher`（OkHttp，手动重定向每跳重校验 + Dns 钩子防 DNS 重绑定）、`SsrfValidator`（60103：同域白名单+私网/回环/CGNAT/ULA 拦截）、`SourceRateLimiter`（并发位+滑动窗口）、`ErrorCode` 补 6xxxx 段、`GET /api/v1/internal/source-reading/{id}/search\|detail`；**O9 已解决**（绕逻辑删除查询 + 恢复语义） |
| 切片 2 · 子片 2b：GraalJS 脚本执行 | ✅ 已完成（`mvn test` 通过，reading 58 测试全绿） | GraalJS 24.1.2（polyglot + js-community，62MB 依赖树记技术债）、`JsScriptEngine`（共享 Engine + 独立 Context 沙箱 + `Context.interrupt` 看门狗 + `ResourceLimits` 语句数双保险）、`JsBridge`（§7.2 P0 allowlist：ajax/get/put/crypto，`java.ajax` 走 HttpFetcher 统一出口，UI 桥 no-op，allowlist 外 fail-fast）、`RuleExecutor` ScriptStep 接线；AC 验证：死循环被看门狗中断不钉线程、`Java.type` 沙箱拒绝、SSRF 经桥仍拦截 |
| 切片 3 · 子片 1：内容发现与作品入库 | ✅ 已完成（`mvn test` 通过，reading 68 测试全绿） | `reading_work` / `reading_work_source_binding` 表、`Work` / `WorkSourceBinding` 实体、`MatchKeys`（保守归并键）、`WorkAssemblyService`（find-or-create + 跨源 merged + 幂等重绑）、`ContentDiscoveryService` 编排搜索→入库、`POST /api/v1/internal/ops/works/discover` |
| 切片 3 · 子片 2：目录与章节资产化 | ✅ 已完成（`mvn test` 通过） | `reading_chapter` / `reading_chapter_source_binding` 表、`Chapter` / `ChapterSourceBinding` 实体、`SourceReadingService.toc`、`ChapterAssemblyService`（主来源建统一章节+回填 chapterId、非主来源按目录序与标题惰性对齐、空目录保护、先清后建）、`ChapterSyncService` 编排、`POST /ops/works/{workId}/toc-sync` |
| 切片 4：正文抓取与净化 | ✅ 已完成（`mvn test` 通过，reading 94 测试全绿） | 子片 1：`reading_chapter_content_snapshot` 表、`ContentFetchService`（cache-first + 三层 raw/normalized）、`ContentNormalizer`。子片 2：`reading_content_sanitization_run` 表、`SanitizationPipeline`（七段：normalize/detect-noise/transform/quality/publish + trace）、`ContentSanitizeService`（accepted/degraded 发布 sanitized、rejected 不覆盖、archive-run）、`POST /ops/chapters/{id}/content-fetch\|sanitize` |
| 切片 5：聚合搜索与统一阅读 API | ✅ 已完成（`mvn test` 通过，reading 102 测试全绿） | `ReadingController` 对外 `/api/v1/reading`（search/works/detail/sources/chapters/content），`ReadingQueryService`（mode=aggregate/source、category/completionStatus 过滤、章节 force-refresh 触发目录同步），`ReadingContentService`（latest=sanitized、cache-first 缺净化时复用 raw/normalized 净化、force-refresh 重抓重净化）、读侧 Mapper/VO、网关 `/reading/**` StripPrefix；用户侧 VO 不暴露源站 URL |
| 切片 6：同步任务与重处理 | ✅ 已完成（`mvn test` 通过，reading 119 测试全绿） | `reading_task` 表、`ReadingTask` 实体/Mapper、`ReadingTaskService`（submit/get/page/cancel/acquire/success/partial/failure retry）、`ReadingTaskExecutor`（复用 import/compile/discover/toc/fetch/sanitize 既有服务）、`ReadingTaskWorker`（数据库抢占 + 手动/可选定时 drain，默认关闭）、`POST/GET /api/v1/internal/ops/tasks/**` |
