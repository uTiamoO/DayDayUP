# 阅读平台 v3.0 重构实施计划

> 任务：大灰狼竞品对标后的阅读中台 v3.0 规划/设计补齐
> 日期：2026-07-09
> 范围：本文件是后续代码迁移执行计划；当前任务只创建规划文档，不修改代码。

## 1. 执行原则

1. **先冻结路线和契约，再迁移代码**：先确保 PRD、API spec、design、implement 对 v3.0 方向一致，再进入代码切片。
2. **从旁路到主路**：新增 SourceProfile / Parser / Orchestrator 等新边界先旁路落地，再逐步接管 Public API。
3. **兼容旧链路**：既有 RuleModel-first 能力保留为 legacy adapter、迁移分析和回归样例，避免一次性破坏已完成切片。
4. **每片可验证、可回滚**：每个切片都有明确验证命令、灰度/feature flag 或数据回滚点。
5. **不扩一期边界**：第一阶段仍只做文字小说聚合搜索与阅读，不加入漫画、听书、短剧、复杂权限中心、公开商业化门户。

## 2. 切片总览

| 切片 | 目标 | 主要交付 | 优先级 |
| ---- | ---- | ---- | ---- |
| S0 | 路线/spec 冻结 | v3.0 PRD/spec/design/implement 一致，接口边界冻结 | P0 |
| S1 | SourceProfile 建模 | 新来源资产模型、迁移映射、ops 草案 | P0 |
| S2 | SourceAdapter/Parser 骨架 | 自研 adapter/parser 接口、legacy adapter 桥接 | P0/P1 |
| S2A | 69shuba 首站 native parser | fixture 采集、challenge 检测、首个 SourceProfile/Parser 示例、合同测试 | P0/P1 |
| S3 | Aggregation Orchestrator | 多源并发编排、source_time_cost、降级 | P0/P1 |
| S4 | 内容资产流水线升级 | raw/normalized/sanitized 版本追溯、质量评分 | P1 |
| S5 | 缓存/任务/健康治理 | 热点缓存、补偿任务、来源健康、熔断禁用 | P1 |
| S6 | Public API 切换 | Reading API 读侧接入 orchestrator，保持旧契约兼容 | P1 |
| S7 | Access Governance | user_key、正文访问限流、在线 IP/设备、封禁审计 | P1 |
| S8 | Distribution | 自研源配置/规则包分发、版本、更新日志、导入链接 | P1/P2 |
| S9 | 轻量用户中心与状态页 | 用户密钥/统计/设备、服务/来源健康视图 | P2 |
| S10 | Legacy 收敛 | RuleModel-first 主线降级/删除，保留内部工具或归档 | P2 |

## 3. 详细实施切片

### S0. 路线/spec 冻结

目标：冻结 v3.0 从零重构方向，确保后续代码不再沿“直接导入执行外部书源”继续扩展。

任务：

- 对齐 `PRD/novel_aggregator_prd.md`、`PRD/dahuilang_competitor_analysis.md`、`.trellis/spec/backend/reading-api-contracts.md`、本 `design.md`、本 `implement.md`。
- 明确 Open-source Reading/Legado 只作为参考资料。
- 冻结第一期 Public API：搜索、作品、详情、来源、章节、正文。
- 冻结 Internal API 边界：source-reading/ops 不经外部网关暴露。
- 标注与 v3.0 冲突的旧 RuleModel-first 内容为历史资产或迁移参考。

验证命令：

```bash
git diff --check -- .trellis/tasks/07-08-dahuilang-competitor-analysis/design.md .trellis/tasks/07-08-dahuilang-competitor-analysis/implement.md PRD/novel_aggregator_prd.md PRD/dahuilang_competitor_analysis.md .trellis/spec/backend/reading-api-contracts.md
```

回滚点：

- 仅文档变更，可直接回滚 S0 文档提交。
- 如后续评审否决 v3.0，恢复 v2.0 RuleModel-first PRD/spec，并保留竞品分析为参考文档。

### S1. SourceProfile 建模

目标：建立 DayDayUP 自研来源资产根模型，替代直接以 Legado 原始书源作为生产模型。

任务：

- 新增 SourceProfile 领域模型、表结构、Mapper、Service。
- 建立 SourceProfile 与旧 `reading_source_definition` 的迁移映射。
- 增加 profile 状态、优先级、限速、host allowlist、parser strategy reference、health summary 字段。
- 新增内部 ops 查询/创建/禁用/恢复接口草案。
- 新增只读迁移工具：从旧 source definition 生成 SourceProfile draft，不自动启用生产。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
```

回滚点：

- 新表并行存在，不修改旧表语义。
- 删除 SourceProfile feature flag 或禁用新 ops 入口即可回旧链路。
- Flyway/SQL 需提供 down/人工回滚说明：删除新表前先确认无新 profile 被 API 使用。

### S2. SourceAdapter / Parser 骨架

目标：建立自研 adapter/parser 抽象，并把旧 RuleModel 执行封装为 legacy adapter。

任务：

- 定义 `SourceAdapter` 接口：search/detail/toc/content。
- 定义 Parser 输入输出 DTO，输出平台结构化类型。
- 实现 `LegacyRuleModelAdapter`，只用于桥接已验证来源和回归样例。
- 实现至少一个 native parser 示例来源，用于证明 v3.0 主线可行。
- 约束所有 adapter 出站必须走统一 HttpFetcher / SSRF / RateLimiter。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
```

重点测试：

- native parser 输出平台 DTO，不暴露源站 URL 到 Public VO。
- legacy adapter 能复用旧测试样例。
- adapter 直接创建 HTTP client 的行为被测试或代码审查禁止。

回滚点：

- 保持 ReadingQueryService 仍走旧服务。
- 新 adapter 未接入 Public API 前可直接关闭新 bean 或 feature flag。

### S2A. 69shuba 首站 native parser

目标：以 69shuba 作为 v3.0 第一个具体站点样例，验证 SourceProfile + native Parser + diagnostics + blocked handling 的完整合同；在无法直接访问业务 HTML 时先落地 fixture 与 `verification_required` 行为，不绕过 Cloudflare/Turnstile。

输入资料：

- `.trellis/tasks/07-08-dahuilang-competitor-analysis/research/69shuba-site-analysis.md` 是首站事实来源。
- `docs/书源/69shuba.json` 只能作为 URL/selector 假设参考，不允许直接作为生产执行规则。
- 当前直连 `https://www.69shuba.com/`、搜索、列表、detail、TOC、content 均观测到 Cloudflare HTTP 403 challenge；首站切片必须显式覆盖 blocked/verification_required。

任务：

1. **Fixture 采集与治理**
   - 建立测试 fixture 目录与命名约定，至少包含：Cloudflare 403 challenge、搜索/列表、detail、TOC、content、empty content。
   - unblocked 业务 fixture 必须来自允许的手工采集或可审计路径；不得保存 cookie、token、Turnstile 结果、登录凭据或可复用 challenge 产物。
   - fixture 中出现的上游 URL 只用于内部 source ref 测试；Public VO 断言不得包含这些 URL。
2. **SourceProfile 草案落地**
   - 定义 `sourceCode=69shuba`、`sourceName=69书吧`、`allowedHosts=www.69shuba.com`、`contentType=text_novel`。
   - search 入口为 `/modules/article/search.php`，请求体 `searchkey=<GBK keyword>&searchtype=all`。
   - discovery 入口覆盖 `/novels/{sort}_{categoryCode}_{statusCode}_{page}.htm`；detail 为 `/book/{bookId}.htm`；TOC/content URL 必须从 fixture HTML 提取。
   - 初始健康/启用状态为 `verification_required` 或 `degraded`，直到 fixture 测试证明可解析。
3. **Native Parser 实现**
   - 实现搜索/列表/detail/TOC/content parser，输出 DayDayUP 标准 Source DTO，不输出 Legado 原始对象。
   - selector 先按研究文档中的假设实现，但每个 selector 都必须有 fixture 测试锁定。
   - status 归一化：`完本` / `全本` / statusCode `1` -> `completed`；`连载` / statusCode `2` -> `ongoing`；未知 -> `unknown`。
   - source-specific cleanup 仅移除 `本章完`、69shuba 域名广告、首发域名提示和 `loadAdv(...)` 残留；不得吞正文。
4. **Blocked / verification_required handling**
   - 在 selector 前检测 HTTP 403、`server: cloudflare`、`Just a moment...`、`challenges.cloudflare.com`、`challenge-platform`、`turnstile`、`cf-ray`。
   - 命中 challenge 时返回 source diagnostic：`status=blocked` 或 `verification_required`、`resultCount=0`、`errorReason=cloudflare_challenge`、`httpStatus=403`。
   - 不自动重试绕过，不注入人工 cookie，不复用浏览器验证态，不将 challenge HTML 作为 parse_error。
5. **Diagnostics 与 Health**
   - 每次 search/discovery/detail/toc/content 都记录 source id/name、elapsed、cacheHit、resultCount、status、errorCode/disabledReason。
   - Public diagnostics 只保留安全摘要；完整 header、HTML、cf-ray 等仅在内部日志/ops 中按敏感等级处理。
6. **Public API 安全**
   - 69shuba work/chapter 上游 URL、header、cookie、原始 selector、challenge body 不得出现在 Public VO。
   - Public response 只暴露 platform workId/chapterId/sourceId/sourceName/title/author/category/status/latestChapterName/sanitized content 等标准字段。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
```

重点测试：

- Challenge fixture 返回 `blocked` / `verification_required`，不执行成功 selector，不被记为普通 parse_error。
- 搜索请求 keyword `斗破苍穹` 的表单体按 GBK 编码，并包含 `searchtype=all`。
- 分类 URL 生成：category `9`、status `2`、page `3`、sort `monthvisit` -> `/novels/monthvisit_9_2_3.htm`。
- `/book/58687.htm` 提取 sourceWorkKey `58687`；相对 URL 均按 `https://www.69shuba.com` 规范化并仅内部保存。
- detail fixture 能解析 title/author/category/status/updateTime/latestChapter/tags/intro/TOC href。
- TOC fixture 按 DOM 顺序生成章节 ordinal。
- content cleanup 删除 `www.69shuba.com`、首发域名提示、`loadAdv(1, 2);`，保留中文正文段落。
- `.txtnav` 缺失或 cleanup 后为空映射为 `READING_CONTENT_EMPTY` 或既有内容空错误合同。
- Public VO 测试断言不包含 `sourceBookUrl`、`sourceChapterUrl`、上游 header/cookie/challenge body。

阻塞与降级：

- 若只有 Cloudflare challenge fixture，仍可完成 challenge 检测、SourceProfile 草案、请求编码和 diagnostics 测试；业务 parser 标记为 `verification_required`，不宣称生产可用。
- 若 unblocked fixture 缺失，不允许把 `docs/书源/69shuba.json` 的 selector 视为已验证生产合同。
- 后续获取合法 fixture 后再把该 source 从 `verification_required` 提升到灰度 enabled。

回滚点：

- 69shuba SourceProfile 默认为 disabled/degraded，不影响现有 ReadingQueryService。
- 关闭 native 69shuba adapter bean 或 feature flag 即可退出首站实验。
- 删除/回滚 69shuba fixture 与 parser 不影响 SourceAdapter 通用骨架。

### S3. Aggregation Orchestrator

目标：实现多源聚合编排和 `source_time_cost` 类诊断，形成 v3.0 核心能力。

任务：

- 新增 orchestrator service：选源、并发执行、超时控制、部分失败降级。
- 引入 per-source diagnostics model：sourceId/sourceName/elapsed/cacheHit/resultCount/status/errorCode/disabledReason。
- 聚合搜索返回候选结果、命中来源数、disabled sources、diagnostics。
- 接入 source health 状态，跳过 disabled/circuit-open 来源。
- 聚合与后台任务共用 per-source 限速器。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
```

重点测试：

- 单源超时不导致整体失败。
- disabled source 出现在 diagnostics，不被执行。
- cache hit 也记录 elapsed/cacheHit/resultCount。
- 所有 sources 都失败时返回明确阅读域错误。

回滚点：

- orchestrator 先只挂 internal preview API。
- Public API 仍可通过 feature flag 保持旧 search。

### S4. 内容资产流水线升级

目标：把正文处理升级为可追溯、可回放、可质量治理的内容资产流水线。

任务：

- 补充 content asset 与 SourceProfile/Parser/Sanitizer version 的关联。
- 完善 raw/normalized/sanitized 三层版本语义。
- 增加质量评分、低质原因、低质不覆盖已发布 sanitized 的策略。
- 增加广告移除 trace 和 sanitizer version。
- 支持按版本重跑净化任务。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
```

重点测试：

- rejected/low quality 不覆盖上一版 sanitized。
- raw 存在时 cache-first 不重复回源。
- sanitizer version 变化可重跑并生成新记录。

回滚点：

- 保留旧 content snapshot 字段。
- 读取端默认仍可读取上一 sanitized。
- 质量评分只记录不拦截作为灰度阶段。

### S5. 缓存 / 任务 / 健康治理

目标：形成高性能与高可用基础设施闭环。

任务：

- 设计搜索/详情/目录/正文缓存 key，包含 source/profile/parser/version 维度。
- 增加 cache hit/miss 指标并写入 diagnostics。
- 扩展 task 类型：health_probe、cache_warmup、distribution_build、profile_migration。
- 扩展 health 记录：成功率、P95、连续失败、最近错误、熔断状态。
- 实现手动禁用/恢复、探活、慢源排行 ops 能力。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
```

重点测试：

- 未来 `nextRunAt` 任务不会被提前执行。
- stale lock 仍可恢复。
- health disabled 后 orchestrator 不再调用该 source。
- 清理某 profile/version 缓存不影响旧版本缓存。

回滚点：

- task worker 默认保持关闭。
- health 熔断先只记录不自动禁用，再逐步打开 enforcement。
- 缓存可按 profile/version 清理。

### S6. Public API 切换

目标：把现有 Reading Public API 读侧逐步切到 Aggregation Orchestrator，同时保持外部契约稳定。

任务：

- 在 ReadingQueryService 引入 orchestrator behind feature flag。
- `mode=aggregate` 使用 orchestrator；`mode=source` 支持指定 SourceProfile/legacy source。
- 搜索响应增加聚合摘要和可控 diagnostics 字段；敏感诊断仅 ops 可见。
- 详情/目录/正文指定 sourceId 路径接入 adapter/orchestrator。
- 确认 Public VO 仍不暴露 sourceBookUrl/sourceChapterUrl/header/cookie。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-access/daydayup-gateway -am test -DskipITs
```

重点测试：

- 旧 API 路径和分页字段不破坏。
- `/api/v1/internal/**` 不经 gateway 暴露。
- diagnostics 不泄露源站敏感信息。
- feature flag 关闭后恢复旧 ReadingQueryService 行为。

回滚点：

- 一键关闭 orchestrator feature flag。
- 保留旧 service bean 和旧查询路径至少一个版本周期。

### S7. Access Governance

目标：围绕正文访问建立 `user_key`、IP/设备、阅读次数、封禁审计闭环。

任务：

- 新增 user_key、membership、access session、read counter、ban audit 模型。
- 正文 API 增加 access context 解析，灰度期允许匿名/内部调用。
- 实现在线状态刷新：正文访问即在线，超时离线策略配置化。
- 实现 IP/设备并发限制，普通/VIP/SVIP 上限配置化。
- 实现封禁审计、读数统计、轻量查询接口。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
```

重点测试：

- 缓存命中正文也执行 access governance。
- 超限/封禁/过期返回明确错误码。
- audit-only 模式只记录不拦截。
- 不记录明文可复用敏感凭据。

回滚点：

- enforcement flag 关闭，仅保留 audit。
- user_key 检查可按来源/调用方灰度。
- 封禁规则先人工触发，自动封禁后置。

### S8. Distribution

目标：建设 DayDayUP 自研源配置/规则包分发中心，提供版本和更新日志。

任务：

- 新增 distribution package、package item、changelog、visibility 模型。
- 构建源配置包，包含 SourceProfile、Parser strategy reference、sanitizer version、checksum。
- 支持 Reading/Legado 兼容导入协议链接，但链接指向 DayDayUP 审计包。
- 支持普通/VIP/内部灰度可见性。
- 支持分发指针回滚到上一稳定包。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
```

重点测试：

- 包版本不可变。
- checksum 可验证。
- 外部原始未审计书源不会直接进入生产包。
- 回滚分发指针后客户端拿到旧稳定版本。

回滚点：

- 分发入口可关闭。
- 包指针回滚到上一版本。
- 删除错误包的可见性，不物理删除审计记录。

### S9. 轻量用户中心与状态页

目标：提供对标竞品但不复杂化的一组自用/运营视图。

任务：

- 用户中心查询：user_key、会员状态、阅读统计、在线 IP/设备、封禁状态。
- 状态页查询：服务健康、来源健康、禁用源、慢源排行、最近错误。
- 管理动作：清理在线设备、手动封禁/解封、恢复来源、触发探活。
- 保持内网/运营边界，不建设公开商业门户。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-access/daydayup-gateway -am test -DskipITs
```

重点测试：

- 用户中心不暴露明文敏感凭据。
- internal ops 不被 gateway `/reading/**` 暴露。
- 设备清理/封禁动作有审计。

回滚点：

- UI/API 可按 feature flag 隐藏。
- 管理动作可改为只读模式。

### S10. Legacy 收敛

目标：在新主线稳定后，收敛 RuleModel-first 历史实现，降低长期维护成本。

任务：

- 统计仍依赖 LegacyRuleModelAdapter 的来源和接口。
- 将核心来源迁移到 native SourceProfile + Parser。
- 移除 Public API 对 RuleModel 概念的直接依赖。
- RuleModel 编译/执行保留为内部迁移工具，或归档删除。
- 更新 spec，明确 legacy 能力的保留/废弃策略。

验证命令：

```bash
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test -DskipITs
JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn test-compile -DskipTests
```

重点测试：

- Public API 不再依赖 RuleModel 命名或字段。
- legacy 样例仍可作为迁移工具运行，或删除后测试同步移除。
- 文档/spec 与代码状态一致。

回滚点：

- 保留 legacy adapter 一个版本周期。
- 删除前打 tag 或独立提交，必要时恢复 legacy 包。

## 4. 横向检查清单

每个代码切片完成后至少检查：

- Public VO 不暴露源站 URL、header、cookie、内部脚本内容。
- 所有出站请求都走 HttpFetcher / SSRF / RateLimiter。
- 聚合与任务共用限速治理。
- source 维度诊断至少包含 elapsed、cacheHit、resultCount、status、error/disabled reason。
- `/api/v1/internal/**` 不经 gateway 对外路由。
- feature flag 或配置回滚路径存在。
- 第一阶段未引入漫画、听书、短剧正文能力。

## 5. 当前任务验收

当前任务只交付规划/设计文件，验收方式：

```bash
git diff --check -- .trellis/tasks/07-08-dahuilang-competitor-analysis/design.md .trellis/tasks/07-08-dahuilang-competitor-analysis/implement.md .trellis/spec/backend/reading-api-contracts.md
```

预期变更文件：

- `.trellis/tasks/07-08-dahuilang-competitor-analysis/design.md`
- `.trellis/tasks/07-08-dahuilang-competitor-analysis/implement.md`
- `.trellis/spec/backend/reading-api-contracts.md`

不应修改：

- Java/Kotlin/SQL/YAML 代码文件。
- git commit / push / merge 状态。
