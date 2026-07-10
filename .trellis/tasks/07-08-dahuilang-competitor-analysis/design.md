# 阅读平台 v3.0 从零重构技术设计

> 任务：大灰狼竞品对标后的阅读中台目标同步与 v3.0 重新设计
> 日期：2026-07-09
> 范围：规划/设计文档，不修改代码；后续实现以本设计与 `PRD/novel_aggregator_prd.md`、`.trellis/spec/backend/reading-api-contracts.md` 为输入。

## 1. 设计目标

v3.0 将 `daydayup-reading` 从“RuleModel-first 的开源阅读书源导入/执行链路”调整为“高效、高性能、高可用的小说聚合搜索与阅读平台”。

核心设计目标：

1. **高效率**：来源配置、解析策略、聚合诊断、内容资产和任务补偿都平台化，减少人工逐站排障成本。
2. **高性能**：搜索/详情/目录/正文按缓存优先、并发编排、限速隔离、后台补抓设计；命中缓存正文 P95 目标 ≤ 300ms。
3. **高可用**：单源失败、超时、被封或降级不拖垮整体聚合结果；所有 source 维度状态可观测、可禁用、可恢复。
4. **自研可治理**：开源 Reading/Legado 书源只作为参考资料，生产主线由 DayDayUP 自有 SourceProfile、Parser、Orchestrator、Pipeline、Cache、Health、Access Governance 承担。
5. **访问可治理**：正文访问成为 `user_key`、会员等级、在线 IP/设备、阅读次数、封禁审计的主控制点。
6. **可迁移**：保留既有 RuleModel-first 实现作为历史资产和迁移输入，分阶段替换，不一次性破坏现有已跑通闭环。

## 2. 明确边界

### 2.1 开源 Reading/Legado 书源边界

开源 Reading/Legado 书源在 v3.0 中的定位：

- 可参考：站点覆盖、API 入口、URL 模板、字段命名、header/cookie 形态、客户端导入协议、兼容生态。
- 可用于：迁移分析、样例回归、内部辅助工具、SourceProfile 草稿生成。
- 不作为：生产直接导入并执行的主线、未审计脚本执行契约、对外分发的原始规则包。

### 2.2 DayDayUP 平台责任

DayDayUP 必须自有并治理以下能力：

- 来源身份、限速、健康、禁用、优先级和版本治理。
- 搜索/详情/目录/正文解析策略与字段抽取。
- 作品/章节拆分、保守归并、人工纠错。
- 正文 raw / normalized / sanitized 三层资产。
- 广告移除、内容净化、质量评分、低质隔离。
- 缓存、异步任务、熔断、限速、降级、高可用治理。
- `source_time_cost` 类 per-source 可观测。
- 会员密钥、正文访问限流、在线状态、封禁审计。
- 自研源配置/规则包分发、更新日志、兼容协议入口。

### 2.3 一期不做

- 漫画、听书、短剧正文能力。
- 公开商业化门户与复杂权限中心。
- 客户端下载站、EPUB/下载服务。
- 未审计第三方书源脚本的生产直执行。
- 重型分布式/MQ 架构，除非后续性能验证要求升级。

## 3. 总体架构

```text
Client / Gateway
  -> Public Reading API (/api/v1/reading)
      -> Aggregation Orchestrator
          -> Cache Layer
          -> SourceAdapter / Parser
              -> Unified HttpFetcher / SSRF / RateLimiter
          -> Asset Repository (Work/Chapter/Content)
          -> Diagnostics Collector (source_time_cost)
      -> Access Governance (content read control point)
  -> Internal Ops API (/api/v1/internal/ops)
      -> SourceProfile Management
      -> Health / Task / Cache / Merge Correction
      -> Distribution Management
```

内部边界按 package 或模块职责划分，第一阶段仍可在 `daydayup-reading-biz` 内以模块化单体落地：

| 边界 | 核心职责 | 对外依赖 |
| ---- | ---- | ---- |
| `source` | SourceProfile、来源版本、限速、禁用、健康配置 | repository、ops |
| `adapter` / `parser` | 自研 SourceAdapter、字段抽取、解析策略执行 | runtime/http、pipeline |
| `orchestrator` | 多源选择、并发调度、超时、熔断、结果合并、诊断 | source、adapter、cache |
| `asset` / `repository` | Work/Chapter/Content 资产、绑定、版本、纠错 | mapper/database |
| `pipeline` | 正文标准化、广告移除、净化、质量评分 | asset、task |
| `cache` | 搜索/目录/正文缓存、热 key、TTL、降级缓存 | Redis/local cache |
| `task` | 补抓、重处理、健康巡检、分发构建 | source、adapter、pipeline |
| `health` | 来源健康、服务健康、禁用原因、探活记录 | source、ops |
| `access` | user_key、正文访问会话、IP/设备限制、读数、封禁审计 | api、repository |
| `distribution` | 自研规则包/源配置包版本、更新日志、导入协议链接 | source、ops |
| `api` / `ops` | Public API、Internal Ops API、VO/DTO 边界 | service facade |

## 4. 核心组件设计

### 4.1 SourceProfile

`SourceProfile` 是 v3.0 的来源资产根对象，不等同于 Legado 原始书源。

建议能力：

- 来源身份：`sourceId`、`sourceCode`、`sourceName`、站点域名、内容类型（第一期仅 text novel）。
- 入口定义：search/detail/toc/content 的受控入口、URL 模板或 API 描述。
- 解析策略引用：每个入口绑定 DayDayUP 自研 Parser/Extractor 策略版本。
- 治理参数：优先级、并发上限、最小请求间隔、超时、重试、熔断阈值。
- 安全参数：允许 host、协议限制、header 凭据引用、敏感字段标记。
- 健康状态：enabled/disabled/degraded、禁用原因、最近成功/失败、错误分类。
- 分发信息：是否进入分发包、兼容客户端、普通/VIP 可见性、版本说明。

设计原则：

- 所有出站请求必须由 SourceProfile 约束 host 和安全范围。
- 删除或禁用来源必须能让 orchestrator 立刻绕开该来源。
- SourceProfile 版本变化必须能追溯到内容资产与诊断记录。

### 4.2 SourceAdapter / Parser

`SourceAdapter` 是平台对具体来源的执行门面，`Parser` 是自研解析策略。

建议接口能力：

- `search(profile, query, context) -> SourceSearchResult`
- `detail(profile, sourceWorkRef, context) -> SourceWorkDetail`
- `toc(profile, sourceWorkRef, context) -> SourceToc`
- `content(profile, sourceChapterRef, context) -> SourceContent`

设计原则：

- Adapter 负责把 SourceProfile、请求上下文、缓存策略、诊断上下文串起来。
- Parser 负责字段抽取和结构化输出，不负责跨源聚合、不负责持久化。
- Parser 输出必须是平台类型，不把源站 URL、未审计脚本对象透传到 Public VO。
- 允许保留受控脚本/表达式能力，但必须有 allowlist、超时、资源上限和审计记录。
- 既有 RuleExecutor/GraalJS 只能作为迁移兼容层或内部工具，不再定义生产主干。

### 4.3 Aggregation Orchestrator

聚合编排器是 v3.0 P0/P1 核心。

职责：

1. 根据请求选择候选来源：enabled、类型匹配、健康可用、限速允许、优先级合适。
2. 并发调度多个 SourceAdapter：每源隔离超时、失败、熔断和限速。
3. 读取缓存并决定是否回源：搜索缓存、详情缓存、目录缓存、正文资产缓存。
4. 合并结果：保守归并 Work、章节绑定、多源可用性、候选排序。
5. 输出诊断：`source_time_cost` 类记录，包括 source id/name、elapsed、cacheHit、resultCount、status、disabledReason、errorCode。
6. 降级返回：部分成功即可返回，整体失败才返回阅读域错误。

编排策略：

- 搜索优先保证低延迟和部分结果可用，不等待慢源无限拖延。
- 详情/目录/正文允许指定 `sourceId` 精确回源，避免错误归并影响阅读。
- 聚合结果不强行宣称唯一真相，保留 `single_source` / `merged` / `suspect` 等聚合状态。
- 后台任务与前台 API 共用同一限速器和 HttpFetcher。

### 4.4 Content Asset Pipeline

正文资产流水线负责把来源内容变成可消费资产。

分层：

| 层 | 说明 | 典型用途 |
| ---- | ---- | ---- |
| raw | 源站原始正文或原始响应抽取正文 | 回放、排错、重新净化 |
| normalized | 统一换行、段落、标点、编码、基础结构 | 净化输入、质量评分 |
| sanitized | 去广告、去尾巴、去噪、敏感清理后的默认正文 | Public API 默认输出 |

能力：

- 广告移除：站点特征、通用模板、尾部噪声、重复段落。
- 质量评分：长度、段落密度、乱码、广告占比、重复率、缺章风险。
- 版本追溯：SourceProfile 版本、Parser 版本、Sanitization 版本、内容版本。
- 低质隔离：低于阈值不覆盖已发布 sanitized，可进入重处理任务或人工排查。
- 可回放：规则升级后可批量重跑 normalized/sanitized。

### 4.5 Cache / Task / Health

#### Cache

缓存分层：

- 搜索缓存：keyword + mode + source set + page + filters。
- 详情缓存：workId/sourceId 或 sourceWorkRef。
- 目录缓存：workId/sourceId + toc version。
- 正文缓存：chapterId/sourceId/contentVersion。
- 诊断缓存：短 TTL 保存最近 source_time_cost，供 ops 排查。

原则：

- 缓存 key 不包含明文敏感凭据。
- 缓存命中必须写入 per-source 诊断。
- 缓存降级可用，但不得绕过正文访问治理。

#### Task

任务体系承接：

- 回源补抓、目录同步、正文抓取、正文净化、健康巡检、缓存预热、分发包构建。
- 继续保留数据库抢占、幂等提交、重试、延迟执行、默认 worker 可关闭的安全模式。
- 任务处理器不得直接出站，必须复用 SourceAdapter/HttpFetcher/RateLimiter。

#### Health

健康体系包括：

- 来源健康：成功率、P95、最近错误、禁用原因、熔断状态、缓存命中率。
- 服务健康：CPU/内存/存储/网络可作为 P2 状态页输入，但第一期优先来源健康。
- 运营动作：手动禁用/恢复来源、触发探活、查看失败样例、查看慢源排行。

### 4.6 Access Governance

访问治理以正文 API 为主控制点，对标竞品 `user_key` 防共享闭环。

P1 能力：

- `user_key` 生命周期：生成、启用、禁用、过期、重置、绑定用户/会员等级。
- 正文访问会话：访问正文即刷新在线状态；默认 10 分钟无正文访问视为离线的产品策略可配置。
- IP/设备限制：普通/VIP/SVIP 等级对应并发 IP/设备上限。
- 阅读计数：当日阅读次数、累计阅读次数、最近阅读时间。
- 封禁审计：封禁次数、封禁原因、封禁/解封时间、触发事件、操作人。
- API 行为：密钥无效、超限、封禁、过期返回阅读域访问错误；命中缓存也必须执行治理。

设计边界：

- 不在第一期引入复杂权限中心。
- 不保存或输出可复用敏感凭据。
- 可先以轻量用户中心/运营视图承载，后续再接平台统一身份。

### 4.7 Distribution

分发中心面向自研 SourceProfile/Parser/净化配置包，不分发未审计外部规则作为生产主线。

能力：

- 包版本：version、build time、source count、checksum。
- 更新日志：新增/修复/禁用来源、解析策略变更、净化规则变更。
- 可见性：普通/VIP、内部/公开、自用/灰度。
- 客户端兼容：Reading/Legado 导入协议链接可作为兼容入口，但指向 DayDayUP 审计后的配置包。
- 回滚：保留最近稳定版本，可按包版本回退。

### 4.8 API / Ops 边界

Public API：

- `/api/v1/reading/search`
- `/api/v1/reading/works`
- `/api/v1/reading/works/{workId}`
- `/api/v1/reading/works/{workId}/sources`
- `/api/v1/reading/works/{workId}/chapters`
- `/api/v1/reading/chapters/{chapterId}/content`

Internal Ops API：

- SourceProfile 管理。
- Parser/策略验证。
- 聚合诊断和 source_time_cost 查询。
- 健康、任务、缓存、禁用源、归并纠错。
- 访问治理、分发包管理。

边界约束：

- Public VO 不暴露源站 URL、原始 header、凭据、内部诊断敏感字段。
- `/api/v1/internal/**` 不配置外部网关路由。
- Gateway 只转发 `/reading/**` 到 Public Reading API。

## 5. 数据流

### 5.1 聚合搜索

1. Client 调用 `/api/v1/reading/search?mode=aggregate&keyword=...`。
2. Orchestrator 根据 SourceProfile 选源。
3. 每源先查搜索缓存；未命中则经 SourceAdapter -> HttpFetcher -> Parser。
4. 每源记录 elapsed/cacheHit/resultCount/status/error。
5. Orchestrator 保守归并候选 Work，写入或更新资产绑定。
6. 返回候选列表、分页、聚合摘要和 `source_time_cost` 类诊断。
7. 慢源/失败源进入 health 指标，必要时触发熔断或补偿任务。

### 5.2 正文读取

1. Client 调用 `/api/v1/reading/chapters/{chapterId}/content?sourceId=...`。
2. Access Governance 校验 `user_key`（P1 后启用）、会员状态、封禁、IP/设备上限。
3. 读取 sanitized 正文缓存/资产。
4. 若缺失且策略允许，Orchestrator 指定来源回源抓取 raw，进入 normalize + sanitize。
5. 质量评分合格后发布 sanitized；不合格则保留旧版本或返回低质/空正文错误。
6. 记录阅读次数、在线状态、访问审计。
7. 返回 sanitized，Public 默认不返回 raw/源站 URL。

### 5.3 健康与降级

1. SourceAdapter 每次执行写入 source 维度指标。
2. Health 聚合成功率、错误类型、P95、连续失败次数。
3. 达到阈值后将来源标记 degraded/circuit-open/disabled。
4. Orchestrator 选源时跳过不可用来源，并在诊断中输出 disabled reason。
5. Ops 可人工恢复或触发探活任务。

## 6. 从既有 RuleModel-first 实现迁移

### 6.1 保留资产

既有能力可作为 v3.0 迁移输入：

- `reading_source_definition`：可用于生成 SourceProfile 草稿。
- RuleModel 编译结果：可用于识别入口、字段名、URL 模板、headers、加密/后处理样例。
- RuleExecutor/GraalJS：可作为内部迁移验证器或短期兼容层。
- Work/Chapter/Content 资产：继续作为统一资产基础，但需补充 SourceProfile/Parser 版本语义。
- Task、HttpFetcher、SSRF、RateLimiter、Sanitization：保留并按新边界重命名/适配。

### 6.2 降级与废弃

需要逐步降级为非主线：

- 直接导入外部 Legado JSON 并作为生产执行规则。
- 以 RuleModel 编译状态决定来源是否生产可用。
- Public API 依赖 RuleModel 概念或外部书源字段。
- 未经 DayDayUP 审计的外部脚本执行。

### 6.3 迁移策略

1. **路线/契约冻结**：先冻结 v3.0 PRD、API/spec、设计与实现计划，不改代码行为。
2. **并行建模**：新增 SourceProfile/Parser 抽象，与旧 SourceDefinition/RuleModel 并行存在。
3. **适配层桥接**：短期通过 LegacyRuleModelAdapter 将已验证来源包装为 SourceAdapter，便于回归。
4. **新源优先**：新增来源只走 SourceProfile + 自研 Parser，不再新增 RuleModel-first 主线能力。
5. **链路切换**：Orchestrator 先支持 legacy adapter 与 native adapter 混合；逐步把 Public API 读侧切到 Orchestrator。
6. **资产迁移**：补齐 Work/Chapter/Content 与 SourceProfile/Parser version 关联。
7. **删除主线依赖**：当核心来源完成 native parser 迁移后，RuleModel 编译/执行只保留内部工具或移除。

## 7. 兼容与回滚

| 风险 | 兼容策略 | 回滚点 |
| ---- | ---- | ---- |
| 新 Orchestrator 搜索质量不如旧链路 | 支持按配置切换旧 ReadingQueryService / 新 Orchestrator | 回滚 feature flag，恢复旧 search service |
| SourceProfile 建模不完整 | 保留 LegacyRuleModelAdapter | 单源切回 legacy adapter |
| Parser 净化误伤正文 | raw/sanitized 双层保留，低质不覆盖旧版本 | 回滚 sanitizer 版本，恢复上一 sanitized |
| 缓存污染 | key 带 profile/parser/version，支持按版本清理 | 清理新版本缓存并回旧版本 |
| Access Governance 误封 | P1 灰度启用，仅记录不拦截开始 | 关闭 enforcement，仅保留 audit |
| 分发包错误 | 包版本不可变，保留上一稳定包 | 分发指针回退到上一版本 |

## 8. 附录：首个具体来源 69shuba SourceProfile / Parser 示例

> 依据：`.trellis/tasks/07-08-dahuilang-competitor-analysis/research/69shuba-site-analysis.md`。`docs/书源/69shuba.json` 只能作为 selector 与 URL 模板参考；生产 parser 必须基于 DayDayUP 直接观察或人工允许路径采集的 HTML/API fixture 验证。

### 8.1 SourceProfile 草案

| 字段 | 建议值 / 合同 |
| ---- | ---- |
| `sourceCode` | `69shuba` |
| `sourceName` | `69书吧` |
| `contentType` | `text_novel` |
| `baseUrl` | `https://www.69shuba.com` |
| `allowedHosts` | `www.69shuba.com`；如后续观察到静态图床域名，需单独审核加入 |
| `entry.search` | `/modules/article/search.php`，POST，`searchkey=<GBK keyword>&searchtype=all`；当前环境直连返回 Cloudflare 403，状态为 `verification_required` |
| `entry.discovery` | `/novels/{sort}_{categoryCode}_{statusCode}_{page}.htm`、`/novels/male`、`/novels/female`；当前环境直连返回 Cloudflare 403，需 fixture 验证 |
| `entry.detail` | `/book/{bookId}.htm`；`bookId` 从受控 URL 或 fixture 解析，不接受 Public API 直接传入上游 URL |
| `entry.toc` | 从 detail HTML 的 `.more-btn@href` 或 `.addbtn a:eq(0)@href` 提取；不得在未验证前硬编码 `/txt/{bookId}/` |
| `entry.content` | 从 TOC HTML 的章节链接提取；不得在 Public VO 暴露 |
| `defaultHeaders` | 仅保存非敏感策略：`Referer: https://www.69shuba.com/`、`Accept-Language: zh-CN,zh;q=0.9`；User-Agent 由统一 HttpFetcher 策略提供 |
| `charsetPolicy` | 搜索表单请求体使用 GBK；响应按 `Content-Type` / HTML meta 判定，不因参考 JSON 直接固定生产响应编码 |
| `antiBotPolicy` | 检测 Cloudflare/Turnstile challenge，记录 `blocked` / `verification_required` 诊断；不自动绕过、不存储 cookie/token/Turnstile 结果 |
| `initialStatus` | `degraded` 或 `verification_required`，直到 unblocked fixtures 通过 parser 合同测试后再允许启用生产 |

### 8.2 Native Parser 合同

69shuba parser 作为 v3.0 首个 native parser 示例时，分为五组 parser，并全部输出 DayDayUP 平台 DTO：

| Parser | 输入 fixture | 核心抽取 | 输出约束 |
| ---- | ---- | ---- | ---- |
| `SearchResultParser` | 搜索响应 HTML | `.newbox li`、`h3 a@text`、`.labelbox label:eq(0)@text`、`.ellipsis_2@text`、`.zxzj p@ownText` | 输出 `SourceWorkSearchItem`；上游 work URL 只进入内部 source ref |
| `DiscoveryListParser` | 分类/榜单 HTML | `#article_list_content li` 及列表上下文中的 category/status code | 输出候选 work；status 归一化为 `completed` / `ongoing` / `unknown` |
| `DetailParser` | `/book/{id}.htm` HTML | `[property$=book_name]`、`author`、`image`、`category`、`status`、`update_time`、`.navtxt`、tags JS 片段、TOC href | 输出 `SourceWorkDetail` 与内部 TOC ref；不得把 TOC URL 透传到 Public VO |
| `TocParser` | TOC HTML | `#catalog li a` | 输出 `SourceTocItem`，`ordinal` 使用 DOM 顺序 |
| `ContentParser` | 章节 HTML | `.txtnav@textNodes` | 输出 raw content；随后执行 source-specific cleanup 与通用 sanitizer |

Source-specific cleanup 初始规则必须以 fixture 测试锁定，参考表达式为：

```regex
\s*[（(]?本章完[）)]?\s*$|新.{0,2}书吧|吧书.{0,2}新|请记住本书首发域名.*|www\.69shuba\.com|loadAdv\([\d, ]*\);?
```

清理规则只移除站点尾巴、域名广告和脚本残留；不得移除正文段落。低质或清理后为空时进入内容质量/错误状态，不覆盖既有 sanitized 资产。

### 8.3 Blocked / verification_required 行为

所有 69shuba adapter 调用必须先做 challenge 检测，再运行业务 selector。以下信号任一命中即可短路：

- HTTP `403` 且 `server: cloudflare`。
- HTML 标题包含 `Just a moment...`。
- 响应包含 `https://challenges.cloudflare.com`、`challenge-platform`、`turnstile` 或 `cf-ray`。

短路后的平台行为：

1. 不把 challenge HTML 当作业务 HTML 解析。
2. `source_time_cost` 记录 `status=blocked` 或 `status=verification_required`、`resultCount=0`、`errorReason=cloudflare_challenge`、`httpStatus=403`。
3. Public API 只暴露安全诊断摘要，不暴露上游 header、cookie、challenge token 或完整 HTML。
4. Health 将该来源 search/discovery/detail/toc/content 能力标记为 degraded，不触发自动绕过、cookie 复用、Turnstile 自动化或凭据保存。
5. 后续启用生产前必须补齐直接观察/人工允许采集的 unblocked fixture，并通过 parser 合同测试。

### 8.4 Fixture 与验收要求

首站实现不得以 `docs/书源/69shuba.json` 直接驱动生产执行。至少需要以下 fixture：

- Cloudflare 403 challenge fixture：验证 blocked/verification_required 识别。
- 搜索或分类列表 fixture：验证候选作品抽取、GBK 搜索请求编码、category/status 归一化。
- detail fixture：验证 meta 属性、简介、tags、TOC href 抽取。
- TOC fixture：验证章节顺序与 sourceChapterKey 提取。
- content fixture：验证 `.txtnav` raw 抽取、cleanup、空正文和净化失败路径。

所有 fixture 必须脱敏，不包含 cookie、token、Turnstile 结果或可复用登录凭据。

## 9. 验收关注点

- PRD/spec/design 三者方向一致：v3.0 是自研高性能聚合平台，不是外部书源直执行平台。
- 第一阶段仍保持文字小说边界。
- 设计覆盖 SourceProfile、SourceAdapter/Parser、Orchestrator、Content Asset Pipeline、Cache/Task/Health、Access Governance、Distribution、API/Ops、迁移策略。
- 69shuba 首站示例遵守：参考 JSON 只做输入假设；生产 parser 基于 fixture 验证；Cloudflare/Turnstile 只检测并标记，不绕过、不保存凭据。
- 后续代码切片可按实现计划逐步落地，每片有验证命令与回滚点。
