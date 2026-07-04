# Implementation Plan: 阅读中台切片5：聚合搜索与统一阅读 API

## Pre-check

- 确认当前任务为 `.trellis/tasks/07-04-reading-slice5-unified-api`。
- 阅读顺序：`implement.jsonl`（如有）→ `prd.md` → `design.md` → 本文件。
- 本机 Maven 使用 JDK 21：`JAVA_HOME=C:/Users/98365/.jdks/ms-21.0.9`。

## Steps

1. **补齐 reading-api VO**
   - 新增分页、作品摘要/详情、来源摘要、章节摘要、正文 VO。
   - 保持字段 camelCase，与现有 `R<T>` 契约一致。

2. **补齐 Mapper 读侧查询**
   - `WorkMapper`：作品搜索/列表/详情所需查询。
   - `WorkSourceBindingMapper`：work 来源列表与来源数。
   - `ChapterMapper`：章节分页、最新章节。
   - `ChapterSourceBindingMapper`：章节来源可用性。
   - 尽量使用简单注解 SQL；复杂分页可先用 MyBatis-Plus `QueryWrapper` 在 service 内实现。

3. **实现查询编排服务**
   - 新增 `ReadingQueryService` 与实现。
   - 实现 `search`：source 模式复用 `ContentDiscoveryService.discover`；aggregate 模式查统一作品。
   - 实现 works/detail/sources/chapters。
   - 统一校验 page/pageSize/mode/sourceId。

4. **实现正文编排服务**
   - 新增 `ReadingContentService` 与实现。
   - 实现 latest/sanitized/normalized/raw 版本选择。
   - cache-first 缺失时触发 `ContentFetchService.fetchAndStore` + `ContentSanitizeService.sanitize`。
   - force-refresh 强制重抓重净化。

5. **新增用户侧 Controller**
   - 路径 `/api/v1/reading`。
   - 暴露 PRD AC 中 6 个 GET 接口。
   - 返回 `R.ok(...)`。

6. **补齐网关路由**
   - 在 gateway 配置中新增 `/reading/**` StripPrefix 路由到 reading 服务。
   - 检查不暴露 `/api/v1/internal/**`。

7. **测试**
   - 新增/调整 service 单测：source 搜索、aggregate 搜索、作品详情、章节分页、正文默认 sanitized、force-refresh。
   - 使用 mock/fake mapper/service，不打真实网络。
   - 运行：
     ```bash
     JAVA_HOME=/c/Users/98365/.jdks/ms-21.0.9 mvn -pl daydayup-modules/daydayup-reading/daydayup-reading-biz -am test
     ```

8. **更新 PRD 进度**
   - 若实现与验证通过，更新 `PRD/novel_aggregator_prd.md` §12 切片 5 状态与交付说明。

## Review Gates

- 编辑前确认没有覆盖现有未完成切片 4/其它任务的改动。
- 实现完成后使用 `trellis-check` 或 code review 检查契约、安全边界和测试覆盖。
- 若测试失败，先修复根因再更新进度文档。

## Rollback Points

- VO/Service/Controller 是新增代码，可整包回滚。
- Mapper 查询若影响已有测试，应优先新增方法而非修改既有方法语义。
- 网关路由只新增 reading path；如启动失败，可单独回滚 gateway 配置。
