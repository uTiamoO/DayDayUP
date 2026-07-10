# 竞品大灰狼平台考察与阅读中台目标同步

## Goal

将“大灰狼聚合”平台的竞品考察结果沉淀为项目文档，并同步更新阅读中台 PRD 的目标能力边界，使后续 `daydayup-reading` 切片规划明确对标：多源聚合编排、会员密钥/正文访问限流、规则包分发、轻量用户中心、服务/书源健康展示。

本任务只做文档与产品范围同步，不做代码实现。

## Background / Confirmed Facts

- 用户明确要求考察 `http://219.154.201.122:5006/user`，目标是让我们的阅读中台实现类似功能。
- 已通过静态页面、免登录 API、登录 Cookie 只读接口考察“大灰狼聚合”平台。
- 登录 Cookie 只用于只读接口：`/user_api`、`/get_sy_log`、`/monitor_iframe`、`/api/system_info`、`/qyd`；未调用清理设备、修改密码、修改资料等写接口。
- 竞品由两部分组成：
  - `219.154.201.122:5006`：认证中心、用户中心、书源/软件分发、用户自助管理、系统监控。
  - `api.langge.cf`：公开聚合阅读 Web 应用，提供搜索、发现、详情、目录、正文、书架、下载等能力。
- 竞品核心商业闭环：充值会员 -> 获取 `user_key`/密钥 -> 在阅读类客户端导入书源或填写密钥 -> 访问多源聚合内容 -> 通过在线 IP/设备、阅读统计、封禁进行防共享治理。
- 竞品用户中心页签：用户、书源、软件、管理、监控。
- 竞品用户模型包含：`id`、`email`、`user_key`、`is_vip`、`vip_level`、`vip_start_time`、`vip_end_time`、`balance`、`day_read_count`、`all_read_count`、`last_read_time`、`device`、`ban_count`、`is_banned`、`ban_time`、`unban_time`、`user_ip`、`register_time`。
- 竞品书源分发按客户端生态拆分：阅读/源阅/栖阅/阅读越多、iOS 用心读书、读不舍手/千阅/源阅读、益达、香色闺阁、轻悦时光。
- 竞品书源包区分普通/VIP、完全版/独立版/兼容版，并通过 `yuedu://booksource/importonline?src=...`、`legado://import/auto?src=...` 等协议导入。
- 竞品软件分发包含 Android 阅读、iOS TestFlight/付费客户端、阅读阅多、读不舍手、栖阅、轻阅读全平台，并通过 `/qyd` 分发注册码。
- 竞品管理页包含头像昵称、番茄登录、在线 IP/设备自助管理、修改密码、联系方式/教程。
- 竞品防共享重点落在正文访问链路：访问正文视为在线，10 分钟未访问自动下线；普通/VIP 最多 1 个 IP 同时阅读，SVIP 最多 5 个 IP。
- 竞品监控页通过 `/api/system_info` 暴露 CPU、内存、存储、网络上下行、上下行速率。
- 竞品聚合阅读 API 包含：`/search`、`/discovestyle`、`/discovedata`、`/detail`、`/catalog`、`/content`、`/download`、`/downloadImg`、书架相关接口。
- 竞品 `/search` 返回统一信封：`code`、`msg`、`cache`、`time`、`source_time_cost`、`disabled_sources`、`data`，其中 `source_time_cost` 记录每个来源耗时、缓存命中、结果数量。
- 竞品支持小说、听书、短剧、漫画多内容形态；我们当前阅读中台 PRD v2.0 明确第一期只做文字，漫画/听书/图文第一期不做。
- 现有 `PRD/novel_aggregator_prd.md` 已定位为阅读中台产品需求层，服从 reading 相关 spec，当前第一期范围包含 Legado 导入、RuleModel 编译、搜索/详情/目录/正文、净化、持久化、统一 API、基础运营与任务。
- 现有 PRD 已有聚合搜索、per-source 限速、可观测、安全、合规等方向，但尚未系统纳入竞品暴露出的会员密钥、正文访问限流、规则包分发中心、轻量用户中心、服务/书源健康状态页等产品能力。

## Requirements

- R1. 新增一份竞品考察文档，记录“大灰狼聚合”平台的产品形态、关键页面、API 契约、用户/会员/防共享模型、书源/软件分发生态、系统监控能力、对阅读中台的启发与差距。
- R2. 更新 `PRD/novel_aggregator_prd.md`，把竞品启发转化为阅读中台的产品目标、分期边界或后续演进项。
- R3. PRD 更新必须尊重现有定位：`novel_aggregator_prd.md` 是产品需求层，技术 HOW 仍以 reading spec 为权威；不得把具体实现设计塞入 PRD。
- R4. PRD 更新必须保留第一期“文字阅读 API 中台”的既有边界，不把漫画、听书、短剧、公开商业化、复杂权限中心强行塞入第一期。
- R5. 文档应明确竞品能力的优先级建议：
  - P0/P1：多源聚合编排层与 per-source 可观测。
  - P1：会员密钥与正文访问限流闭环。
  - P1/P2：规则包分发中心与更新日志。
  - P2：轻量用户中心与服务/书源健康状态。
  - P3：多内容形态、客户端下载/EPUB 导出等扩展。
- R6. 文档必须避免保存敏感 Cookie；可以记录字段结构和接口形态，但不落明文 `qttoken`、`deviceId`。
- R7. 文档变更应为后续切片规划提供可执行输入，包括推荐新增或调整的里程碑/交付切片。

## Acceptance Criteria

- [ ] 新增竞品分析文档，包含已考察的公开站点、用户中心、书源/软件/管理/监控、聚合阅读 API、竞品能力矩阵和阅读中台 Gap 分析。
- [ ] `PRD/novel_aggregator_prd.md` 修订记录新增本次竞品对标同步条目。
- [ ] `PRD/novel_aggregator_prd.md` 的项目目标/一期范围/后续演进或里程碑中体现：多源聚合编排、`source_time_cost` 类可观测、正文访问限流、规则包分发、用户中心/服务健康状态。
- [ ] 文档明确第一期不扩大到漫画/听书/短剧正文能力，不改变现有 RuleModel v1 与 reading spec 的权威关系。
- [ ] 文档中不包含用户提供的明文 Cookie 或可复用登录凭据。
- [ ] 文档更新后可通过人工阅读验收：竞品事实、我们现状、差距、建议路线四者能对应起来。

## Scope Decision

- D1. 竞品对标同步允许调整后续切片优先级：多源聚合编排和 per-source 可观测继续优先；会员密钥/正文访问限流、规则包分发作为紧随其后的产品化切片；不强塞进当前已在执行的代码切片。
