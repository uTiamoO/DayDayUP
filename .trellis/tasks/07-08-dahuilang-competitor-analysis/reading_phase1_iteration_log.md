# 阅读中台一期落地细节迭代记录

> 日期：2026-07-08  
> 范围：围绕当前一期“文字阅读 API 中台”的 SQL 初始化、书源导入、统一 API 可消费性、书源切换、分类/筛选与性能细节进行 3 次迭代记录。本文记录真实代码/文档落点，不替代 PRD 与 reading spec。

## 迭代 1：数据库初始化与资产表结构收敛

### 本次内容

- 新增 `daydayup-reading-biz/src/main/resources/db/init.sql`，覆盖当前实体对应核心表：
  - `reading_source_definition`
  - `reading_source_compiled_rule`
  - `reading_work`
  - `reading_work_source_binding`
  - `reading_chapter`
  - `reading_chapter_source_binding`
  - `reading_chapter_content_snapshot`
  - `reading_content_sanitization_run`
  - `reading_task`
- 表结构统一对齐 `BaseEntity`：`id/create_time/update_time/create_by/update_by/deleted`。
- 增加关键唯一键与索引：书源 URL 幂等、作品-来源绑定、章节序、章节-来源绑定、正文快照、任务幂等等。
- 增加 SQL 文件基线测试，防止核心表或关键索引遗漏。

### 验收结果

- 初始化 SQL 已可作为 `daydayup_reading` 建库后的基础 DDL。
- 当前实体字段与表字段保持一致，读写 Mapper 所需索引已覆盖主要查询路径。

### 下一步建议

- 接入真实环境前，用目标 MySQL 版本执行一次 DDL dry-run，并补充 Flyway/Liquibase 版本化迁移策略。
- 后续会员密钥、规则包分发、健康状态页进入切片时，不直接堆到本文件，建议拆分为版本化迁移脚本。

## 迭代 2：书源初始化与自有格式兼容

### 本次内容

- 将默认导入目录从 `docs/书源/spike-samples` 调整为 `docs/书源`，便于初始化当前目录下已有书源。
- 增强 `SourceImportServiceImpl`：
  - 兼容标准 Legado 顶层数组/对象。
  - 兼容项目自有格式：顶层 key -> `sourceName/sourceUrl/sourceType/enable/weight/httpHeaders/searchBook/bookDetail/chapterList/chapterContent/bookWorld`。
  - 将自有格式导入为 `SourceDefinition`，以 `sourceUrl` 作为 `bookSourceUrl` 幂等键。
  - 对 `Authorization/Cookie/token` 等敏感 header 做脱敏后再保存 rawContent，避免把可复用凭据落入数据库种子或导入资产。
- 增加导入兼容测试，覆盖自有格式、字符串 weight、敏感 header 脱敏与标准 Legado 跳过未变更。

### 验收结果

- `docs/书源/maoyankanshu.json` 这类非标准 Legado 顶层结构不会再因缺少 `bookSourceUrl` 被直接跳过。
- 导入仍保持幂等 upsert：新源插入、未变更跳过、软删同 URL 恢复。

### 下一步建议

- 当前“自有格式 -> SourceDefinition”已解决初始化资产问题，但完整运行解析仍应按后续 DSL/RuleModel 扩展切片推进。
- 对包含大量内联 JS/AES 的源，建议增加编译体检中的“自有格式解析等级”和运行时回归样例，避免导入成功但运行不可用被误判为全链路可用。

## 迭代 3：统一 API 可消费性、书源切换与筛选补齐

### 本次内容

- 在已有统一 API 基础上补齐最小产品接口：
  - `GET /api/v1/reading/sources`：可用书源列表，供书源切换与筛选面板使用。
  - `GET /api/v1/reading/categories`：分类筛选元数据。
  - `GET /api/v1/reading/filters`：筛选元数据，支持 `all/category/completionStatus`。
- 保持既有 API：
  - `/api/v1/reading/search`
  - `/api/v1/reading/works`
  - `/api/v1/reading/works/{id}`
  - `/api/v1/reading/works/{id}/sources`
  - `/api/v1/reading/works/{id}/chapters`
  - `/api/v1/reading/chapters/{id}/content`
- 明确书源切换路径：详情页通过 `works/{id}/sources` 获取可切换来源；目录与正文通过 `sourceId` 明确指定来源。
- 继续执行分页上限 `MAX_PAGE_SIZE=100`，新增筛选 limit 上限校验，避免无限制返回。
- 增加服务测试覆盖书源列表、分类/状态筛选。

### 验收结果

- 前端/客户端可以从“书源列表 -> 作品列表/搜索 -> 详情 sources -> 指定 sourceId 目录/正文”形成最小闭环。
- 分类展示与筛选元数据有稳定 API 输出，不需要调用方直接猜测数据库字段。

### 下一步建议

- 聚合搜索的 per-source 诊断数据仍需产品化输出到统一 API 或运营状态页，建议作为下一个可观测增强切片。
- 正文访问的会员密钥、在线 IP 槽位、阅读计数、封禁治理仍未进入本次代码范围，应按 PRD v2.1 切片 8 单独实现。
- 规则包分发中心与更新日志建议作为切片 9，复用导入/编译后的书源资产生成面向客户端的导入包。
