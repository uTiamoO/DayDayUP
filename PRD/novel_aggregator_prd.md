# 阅读聚合服务 产品需求文档 (PRD)

> **本文件定位**：本 PRD 是「文字阅读 API 中台」的**产品需求层**（WHAT / WHY + 对外契约 + 验收标准）。
> **技术设计以下列规格为权威来源（HOW）**，本文不重复设计细节，仅在冲突时以规格为准：
> - `docs/superpowers/specs/2026-07-02-reading-api-platform-design.md`（架构 / 数据模型 / API / 切片，主规格）
> - `docs/superpowers/specs/2026-07-02-reading-rulemodel-v1.md`（RuleModel v1，取代 dsl-v1-schema，切片 1 落地依据）
> - `docs/superpowers/specs/2026-07-02-reading-js-engine-decision.md`（JS 引擎选型 = GraalJS，实测结论）
> - `docs/superpowers/specs/2026-07-02-reading-source-rule-inventory.md`（书源规则普查，编译覆盖率 68%）
>
> 版本：v2.0（对齐阅读中台稿）  |  最后更新：2026-07-03  |  状态：草案 / 待评审

## 修订记录

| 版本 | 日期 | 变更摘要 |
| ---- | ---- | -------- |
| v1.0 | 2026-07-02 | 初稿：`daydayup-novel` 独立聚合服务，直接解析 Legado + 缓存，单表 + 3 API |
| v1.1 | 2026-07-03 | 对齐工程约定（R/BaseEntity/错误码），补安全/NFR/AC；标注书源格式范围 |
| **v2.0** | 2026-07-03 | **确认与「阅读中台」为同一系统；本文重定位为中台产品需求层，全面对齐 reading 命名与架构（编译 RuleModel、全量持久化、8 实体、GraalJS）；技术设计移交 5 份 spec** |

> **重要**：v1.x 曾设计为独立的 `daydayup-novel` 服务（直接解析 + 缓存 + 单表）。经确认与阅读中台是**同一个系统**，该路线作废。命名统一为 `daydayup-reading`，架构改为「编译式模块化单体」，见 §3。

## 术语表

| 术语 | 说明 |
| ---- | ---- |
| Legado（开源阅读） | 外部书源规则格式，v1 编译的目标输入格式 |
| RuleModel | 自研的 Legado-first 内存归一化规则模型（非跨格式 DSL），书源编译产物，见 rulemodel-v1 规格 |
| 书源(SourceDefinition) | 导入系统的一份外部书源资产 |
| Work / Chapter | 平台内部统一作品 / 统一章节（与来源书籍/章节解耦，多源可绑定同一 Work） |
| 聚合模式 / 指定书源模式 | `mode=aggregate`（跨源归并候选）/ `mode=source`（指定 sourceId 直取），两者并存 |
| 净化 Pipeline | 七段式可编排正文净化流程（广告过滤/敏感词修复/质量评估等） |
| raw / normalized / sanitized | 正文三层：原始 / 标准化 / 净化，物理持久化 raw+sanitized |

---

## 1. 项目概述

### 1.1 目标与定位
在现有 Java / Spring 多模块工程内，落地一个**编译式模块化单体**的**文字阅读 API 中台**：以开源阅读（Legado）书源 JSON 为输入，编译成自研 **RuleModel**，经解析运行时抓取搜索/详情/目录/正文，再经可编排净化 Pipeline 生产并持久化高质量正文，最终通过统一 REST API 对外提供搜索、书目、详情、章节、正文能力。

核心价值：把**书源变成平台资产**、**规则变成可执行标准**、**正文变成可回放/可净化/可演进的内容资产**，而非一次性爬虫。

### 1.2 第一期范围（对齐主规格 §1.2 / §13）
**第一期做**：
- 导入并管理 Legado 书源 JSON；编译为内部 RuleModel（含编译覆盖率/降级报告）。
- 搜索、详情、目录、正文抓取（指定书源模式 + 聚合模式）。
- 正文净化（广告过滤、敏感词修复、无意义内容清理），全量持久化 raw + sanitized。
- 统一作品 / 章节 / 正文资产模型；per-source 抓取限速。
- 统一 REST API；半自动同步任务骨架；基础错误模型、审计与日志。

**第一期不做**：阅读器 UI、香色/爱阅书香适配器输出、公开开放平台门户、多租户、复杂权限中心、全站扫描、全局跨源章节对齐、漫画/听书/图文、高级全文检索、重型分布式/MQ 架构。

### 1.3 书源格式范围
- **v1 只做标准 Legado 格式**（`docs/书源/spike-samples/*.json`），编译为 **RuleModel**（Legado-first，忠实复现语义）。
- 仓库中的**香色/自定义中台格式**（`docs/书源/maoyankanshu.json` 等，`parserID/内联纯 JS AES`）作为「接入第二种书源格式时再启用完整 DSL」的**演进位**，v1 不做。
- 导入按 `bookSourceType=0`（文字）过滤；漫画(2)/听书(1)标记范围外，不计 rejected。
- 编译分级：`full` / `degraded`（受控 script）/ `rejected`（依赖 webView 浏览器内核，不做）。普查覆盖率 full+degraded=68%（详见 inventory 规格）。

### 1.4 合规与免责（对齐主规格 §12.3）
- 抓取内容对外分发存在**根本性版权风险**；第一期定位**个人 / 内网自用**，架构不为公开分发过度设计。
- 公开化（阶段 3）**前置条件**：完成内容版权与合规评估，结论为准。
- 正文持久化与对外接口提供配置开关，可整体停用。前端须展示来源与免责声明。

---

## 2. 用户与使用场景

| 角色 | 场景 | 对应 API 层 |
| ---- | ---- | ---- |
| 阅读客户端 / 前端 | 搜索书 → 看详情 → 看目录 → 读正文（净化后） | 统一阅读 API `/api/v1/reading` |
| 运维 / 开发（调试灰度） | 指定 sourceId 定向抓取、看运行诊断 | 书源定向 API `/api/v1/internal/source-reading` |
| 运营 / 治理 | 导入书源、触发编译/同步、重跑净化、健康巡检、归并纠错 | 运营 API `/api/v1/internal/ops` |
| 其它微服务 | 内部聚合调用 | `daydayup-reading-api` Feign facade |

---

## 3. 架构与工程接入（对齐主规格 §2 / §9，本文不展开设计）

- **架构**：编译式模块化单体，第一期单体部署，内部强边界拆分：书源中心 `source` / 规则编译器 `compiler` / 解析运行时 `runtime` / 净化引擎 `pipeline` / 内容资产中心 `repository` / 阅读 API `api` / 运营 `ops` / 任务 `task`。
- **规则路线**：外部规则**不直接执行**，先编译成 RuleModel，再交运行时执行（见 rulemodel-v1 规格）。
- **JS 能力**：`script` 是受控一等能力（41% 范围内源依赖 JS），引擎 = **GraalJS**（可靠超时中断 + 默认沙箱 + ES2023+，见 engine-decision 规格）；crypto 族桥调用编译期收敛为原生 postProcessor。
- **模块**（`daydayup-modules` 下，与 game/social 并列）：
  - `daydayup-reading-api`：对外 DTO、API 契约、OpenAPI 模型、内部 facade。
  - `daydayup-reading-biz`：上述 8 个 package 的实现，含 `ReadingApplication`。
- **数据库**：一服务一库，新建 `daydayup_reading`；entity/mapper 包结构沿用 auth/admin 约定；主键雪花，基础字段对齐 `BaseEntity`（`create_time/update_time/create_by/update_by/deleted`）。
- **网关**：统一阅读 API 经网关 `/reading` 前缀转发（StripPrefix，对照 admin）；`/api/v1/internal/**` **不配置对外路由**（仅内网可达，网关层显式排除，不靠命名约定）。
- **依赖**：沿用 `daydayup-common-*`（web/mybatis/redis/security/swagger/feign/lock/log/xxljob）+ Nacos；第三方 GraalJS(polyglot+js)、Jsoup、jayway JsonPath、Caffeine、OkHttp；GraalJS 62MB 依赖树体积记入技术债跟踪。
- **配置**：抓取敏感项（代理 / 凭据）走 `application-local.yml`（gitignore）+ Nacos dev namespace，启动 `--spring.profiles.active=local`。

> 数据模型（SourceDefinition / SourceCompiledRule / Work / WorkSourceBinding / Chapter / ChapterSourceBinding / ChapterContentSnapshot / ContentSanitizationRun）与净化 Pipeline 七段结构，以主规格 §3 / §7 为准，本 PRD 不重复。

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
| GET | `/works/{workId}` | 作品详情（可用来源摘要） | — |
| GET | `/works/{workId}/sources` | 作品可用来源（可选） | — |
| GET | `/works/{workId}/chapters` | 章节列表 | `sourceId,page,pageSize,refreshPolicy=cache-first\|force-refresh` |
| GET | `/chapters/{chapterId}/content` | 章节正文 | `sourceId,contentVersion=latest\|raw\|normalized\|sanitized,fetchPolicy=cache-first\|force-refresh` |

- 搜索返回：作品候选列表 + 聚合摘要 + 命中来源数 + 分页。**聚合模式返回候选聚合结果，不强行声明唯一真相**（保守归并，见 §5）。
- 正文：**Public 默认返回 `sanitized`**；内部接口可查看其它版本。缺失回源为同步阻塞时整链路上限 ~10s，超时返回 `UPSTREAM_TIMEOUT` 并落补抓任务，客户端可稍后重查。

### 4.2 书源定向 API `/api/v1/internal/source-reading`（内网，调试 / 灰度）
指定 `sourceId` 的 search / detail / toc / content + 运行诊断。

### 4.3 运营与治理 API `/api/v1/internal/ops`（内网）
导入书源、触发编译、查看编译日志、触发同步、重跑净化、健康巡检、手动回源重抓、**作品归并纠错**（解绑 / 重绑 / 拆分作品，同步迁移章节绑定）。

> **对比 v1.x**：原稿以源站明文 `tocUrl/contentUrl` 直传（SSRF 隐患）已废弃；改为 `workId/chapterId + sourceId` 的平台内部主键，真实源站 URL 只在运行时内部持有，从 API 契约层根除 SSRF 暴露面。

---

## 5. 聚合搜索需求（"aggregator" 核心，对齐主规格 §5.2）

- **两种模式并存**：`aggregate`（跨源归并候选）与 `source`（指定源直取）。
- **保守归并**：第一版按「标题 + 作者 + 站点信息」保守匹配；允许多源命中同一本书；`aggregationStatus ∈ {single_source, merged, suspect}`，`suspect` 待人工确认。
- **可纠错**：误归并通过运营 API 解绑 / 重绑 / 拆分修复（同步迁移章节绑定）。
- **限速共享**：聚合调用与后台批量任务**共用** parse-runtime 的 per-source 限速器（每源并发上限 + 最小请求间隔），JS 内 `java.ajax` 也必须过统一 HTTP 出口，不得绕行。
- **排序 / 去重**（产品要求，运行时/归并层实现）：候选排序综合来源优先级、关键词与标题相似度、字段完整度；同一 Work 下多源按可用性与优先级择优。

> 说明：v1.x 的 Levenshtein 打分与 `normalize()` 去重键属实现细节，归入归并/排序模块，具体算法在 design 阶段定义，不在 PRD 硬编码。

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
| 60201 | READING_RULE_COMPILE_FAILED | 规则 | 书源编译失败（rejected） |
| 60202 | READING_RULE_RUNTIME_FAILED | 规则 | RuleModel 运行时执行失败 |
| 60301 | READING_CONTENT_EMPTY | 内容 | 正文为空 |
| 60302 | READING_CONTENT_SANITIZATION_FAILED | 内容 | 净化失败（抓取可能成功） |
| 60303 | READING_CONTENT_QUALITY_LOW | 内容 | 正文质量过低 |
| 60401 | READING_INVALID_ARGUMENT | 请求 | 参数非法 |
| 60402 | READING_UNSUPPORTED_MODE | 请求 | 不支持的调用模式 |

---

## 7. 安全需求

- **SSRF**：对外契约不接收源站明文 URL（§4.3）；运行时出站统一经校验——目标 host 落在书源 `baseUrl` 同域，解析 IP 禁私网 / 回环 / 链路本地，仅 http/https，重定向后再校验；命中拦截返回 `60103`。
- **JS 沙箱**（GraalJS，见 engine-decision §4）：`build()` 默认禁主机类 / IO + 显式桥 allowlist + `Context.interrupt` 看门狗（叠加语句数 `ResourceLimits`）；`java.ajax` 回调统一 HTTP 出口；UI 类桥 no-op。
- **凭据 / 健康**：书源静态 header 中的凭据（如硬编码 JWT）标记并纳入健康巡检；到期即整源失效，`loginUrl` 探活。
- **接口暴露**：`/api/v1/internal/**` 网关不配对外路由。

---

## 8. 非功能需求 (NFR)

| 维度 | 目标 |
| ---- | ---- |
| 性能 | 命中资产库正文 P95 ≤ 300ms；回源正文（抓取+净化）整链路上限 ~10s，超时降级补抓 |
| 并发 / 限速 | per-source 并发上限 + 最小请求间隔，聚合与后台任务共用限速器；外部请求线程池与 Web 隔离 |
| 可用性 | 单源故障不影响整体；编译 rejected / 运行失败隔离到单源；服务可水平扩缩 |
| 一致性 / 可回放 | raw + sanitized 持久化；书源版本、编译版本、净化规则版本、正文版本可追溯回放 |
| 可观测 | 接入 `daydayup-common-log`；埋点源成功率 / 耗时 / 编译等级分布 / 净化质量，暴露 metrics |
| 安全 | §7 全部满足 |
| 合规 | 正文持久化与对外接口可整体开关（§1.4） |

---

## 9. 验收标准 (Acceptance Criteria，对齐主规格 §11 成功标准)

- [ ] `daydayup-reading-api` / `daydayup-reading-biz` 建成，注册进 Nacos，网关 `/reading` 可路由，`/api/v1/internal/**` 确认无对外路由。
- [ ] **书源资产化**：样例书源可导入并编译为 RuleModel，状态可见、日志可追踪；产出 full/degraded/rejected 三级覆盖率报告，实测不低于切片 0 预估（68%）。
- [ ] **指定书源链路**：篱笆文学（HTML）与猫眼看书（JSON + crypto）两个固定回归样例，稳定完成搜索 / 详情 / 目录 / 正文。
- [ ] **正文净化可见效**：广告移除、尾巴清理、敏感词修复生效，且净化结果（trace / 质量分 / 规则版本）有记录。
- [ ] **统一 API 可消费**：调用方不关心书源细节即可完成 search / works / detail / chapters / content；Public 默认返回 sanitized。
- [ ] **内容资产可回放**：可查看 raw / normalized / sanitized / 净化记录 / 规则版本 / 来源版本。
- [ ] **聚合与纠错**：聚合搜索返回候选与命中来源数；误归并可经运营 API 解绑 / 重绑 / 拆分修复。
- [ ] **安全**：伪造私网 / webView 目标被拦截返回 `60103`；书源 JS 死循环被 GraalJS 看门狗中断，不钉住线程。
- [ ] **限速**：JS 内 `java.ajax` 与后台批量抓取均经 per-source 限速器，无绕行。
- [ ] **响应契约**：所有接口返回 `R{code,message,data,timestamp}`，错误码落 6xxxx 段。

---

## 10. 里程碑 / 交付切片（对齐主规格 §10，按可交付闭环）

| 切片 | 目标 | 关键交付 |
| ---- | ---- | ---- |
| **切片 0**（已闭环） | 编译路线可行性验证 | 覆盖率普查 68%、JS 引擎选定 GraalJS、双加密变体端到端跑通 |
| 切片 1 | 书源导入与编译闭环（最重） | SourceDefinition、自研解析器→RuleModel、原生收敛、编译体检报告 API；前置=RuleModel v1 字段级 schema |
| 切片 2 | 指定书源搜索 + 详情 | RuleModel 运行时执行、基础诊断 |
| 切片 3 | 目录与章节资产化 | Work / Binding / Chapter / ChapterSourceBinding、目录抓取入库 |
| 切片 4 | 正文抓取与净化 | ContentSnapshot、SanitizationRun、三层存储、第一版净化 Pipeline、正文 API |
| 切片 5 | 聚合搜索与统一阅读 API | 聚合搜索、统一列表 / 详情 / 章节 / 正文输出 |
| 切片 6 | 同步任务与重处理 | 任务表、导入 / 编译 / 目录同步 / 补抓 / 净化重跑任务 |

---

## 11. 开放问题 (Open Questions)

| # | 问题 | 状态 |
| ---- | ---- | ---- |
| O1 | 与「阅读中台」的关系 | **已定**：同一系统，本 PRD 服从中台 5 份 spec，复用其规划 |
| O3 | 阅读域错误码段位 | **已定**：6xxxx，不冲突；✅ 已补入 `ErrorCode` 枚举（60001~60402） |
| O6 | 文件与模块归位 | 建议将本文件更名为 `reading_platform_prd.md` 并入 reading 轨；模块 `daydayup-reading-*`。待确认后可执行 git mv |
| O7 | 香色 / 第二种书源格式接入时机 | 演进位保留，触发点=完整 DSL 升级；v1 不做 |
| O5 | 正文缓存 / 持久化默认策略与版权 | 默认开关与 TTL 需产品 / 法务拍板；阶段 3 公开化前置版权评估 |
| O8 | RuleModel v1 字段级 schema 细化规格 | 切片 1 前置依赖，待在切片 0 结论基础上编写（rulemodel-v1 已给结构，需补 postProcessor 清单等字段级定义） |
| O9 | 逻辑删除与书源唯一键交互 | ✅ **已解决**：导入查询绕逻辑删除（`selectByUrlIncludeDeleted`），软删同 URL 重导入走「恢复」语义（deleted 置回 0，status/priority 重置为书源自身值） |
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
| 切片 3 · 子片 2：目录与章节资产化 | ✅ 已完成（`mvn test` 通过，reading 75 测试全绿） | `reading_chapter` / `reading_chapter_source_binding` 表、`Chapter` / `ChapterSourceBinding` 实体、`SourceReadingService.toc`、`ChapterAssemblyService`（主来源建统一章节+回填 chapterId、非主来源仅挂绑定、空目录保护、先清后建）、`ChapterSyncService` 编排、`POST /ops/works/{workId}/toc-sync` |
| 切片 4：正文抓取与净化 | ✅ 已完成（`mvn test` 通过，reading 94 测试全绿） | 子片 1：`reading_chapter_content_snapshot` 表、`ContentFetchService`（cache-first + 三层 raw/normalized）、`ContentNormalizer`。子片 2：`reading_content_sanitization_run` 表、`SanitizationPipeline`（七段：normalize/detect-noise/transform/quality/publish + trace）、`ContentSanitizeService`（accepted/degraded 发布 sanitized、rejected 不覆盖、archive-run）、`POST /ops/chapters/{id}/content-fetch\|sanitize` |
| 切片 5：聚合搜索与统一阅读 API | ✅ 已完成（`mvn test` 通过，reading 102 测试全绿） | `ReadingController` 对外 `/api/v1/reading`（search/works/detail/sources/chapters/content），`ReadingQueryService`（mode=aggregate/source、category/completionStatus 过滤、章节 force-refresh 触发目录同步），`ReadingContentService`（latest=sanitized、cache-first 缺净化时复用 raw/normalized 净化、force-refresh 重抓重净化）、读侧 Mapper/VO、网关 `/reading/**` StripPrefix；用户侧 VO 不暴露源站 URL |
| 切片 6：同步任务与重处理 | ✅ 已完成（`mvn test` 通过，reading 119 测试全绿） | `reading_task` 表、`ReadingTask` 实体/Mapper、`ReadingTaskService`（submit/get/page/cancel/acquire/success/partial/failure retry）、`ReadingTaskExecutor`（复用 import/compile/discover/toc/fetch/sanitize 既有服务）、`ReadingTaskWorker`（数据库抢占 + 手动/可选定时 drain，默认关闭）、`POST/GET /api/v1/internal/ops/tasks/**` |
