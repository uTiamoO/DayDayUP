-- DayDayUP Reading · 69shuba（69书吧）原生书源注册与样例种子数据
-- Target database: daydayup_reading（先执行 init.sql 建表，再执行本文件）
--
-- 用途：
--   1) 注册 69shuba 为「原生书源」(origin_type='native')，runtime 通过 tags='69shuba'
--      或 book_source_url 命中 ShubaSourceParser，走自研解析主线（非 Legado RuleModel）。
--   2) 预置少量样例作品/章节/正文，使聚合读侧 API 立即可查数据：
--        GET /api/v1/reading/search?mode=aggregate&keyword=...
--        GET /api/v1/reading/works        /works/{id}    /works/{id}/sources
--        GET /api/v1/reading/works/{id}/chapters?sourceId=90010001
--        GET /api/v1/reading/chapters/{id}/content?sourceId=90010001
--
-- 说明：
--   - 固定使用 9001xxxx 段主键，避免与雪花 ID 冲突，便于重复执行前手工清理。
--   - source_book_url / source_chapter_url 为内部源引用，Public VO 不暴露（由装配层保证）。
--   - 样例正文已直接落 sanitized_content，故 /content?contentVersion=sanitized 无需回源即可返回。
--   - Cloudflare 拦截时线上「source 模式」实时抓取会返回 60103（verification_required）；
--     本种子保证 aggregate 读侧不依赖实时抓取即可查询。

-- ── 1. 注册原生书源 ────────────────────────────────────────────────
INSERT INTO reading_source_definition
    (id, name, site_name, book_source_url, book_source_type, origin_type, origin_path,
     status, priority, tags, compile_grade, fingerprint, imported_at, raw_content,
     create_time, update_time, deleted)
VALUES
    (90010001, '69书吧', '🌙 小说', 'https://www.69shuba.com', 0, 'native', 'native:69shuba',
     1, 100, '69shuba', 'full', REPEAT('0', 64), NOW(), '{"nativeParser":"69shuba"}',
     NOW(), NOW(), 0)
AS new
ON DUPLICATE KEY UPDATE
    name = new.name,
    site_name = new.site_name,
    origin_type = new.origin_type,
    origin_path = new.origin_path,
    tags = new.tags,
    compile_grade = new.compile_grade,
    update_time = NOW(),
    deleted = 0;

-- ── 2. 样例统一作品 ────────────────────────────────────────────────
-- match_key = MatchKeys.of(title, author) 的规整结果（NFKC + 去空白/标点 + 小写 + SOH 分隔）。
-- 这里以「规整标题 +  + 规整作者」写死；样例标题/作者均为半角，规整后即原文小写去空白。
INSERT INTO reading_work
    (id, title, author_name, category_name, cover_url, description, completion_status,
     word_count, latest_chapter_title, latest_chapter_updated_at, aggregation_status,
     match_key, source_count, create_time, update_time, deleted)
VALUES
    (90010101, '玄鉴仙族', '一觉睡到自然醒', '玄幻魔法', NULL,
     '一个平凡少年携带玄鉴,在仙族林立的世界一步步崛起的故事。', 'serial',
     3200000, '第一千零一章 峰回路转', NOW(), 'single_source',
     CONCAT('玄鉴仙族', CHAR(1), '一觉睡到自然醒'), 1, NOW(), NOW(), 0),
    (90010102, '万相之王', '天蚕土豆', '玄幻魔法', NULL,
     '天上诸神并列,万相林立,少年自微末而起,横推万相的热血传奇。', 'completed',
     4500000, '第八百八十八章 终章', NOW(), 'single_source',
     CONCAT('万相之王', CHAR(1), '天蚕土豆'), 1, NOW(), NOW(), 0)
AS new
ON DUPLICATE KEY UPDATE
    title = new.title,
    author_name = new.author_name,
    category_name = new.category_name,
    completion_status = new.completion_status,
    latest_chapter_title = new.latest_chapter_title,
    update_time = NOW(),
    deleted = 0;

-- ── 3. 作品-来源绑定（都绑定到 69shuba，主来源） ──────────────────
INSERT INTO reading_work_source_binding
    (id, work_id, source_id, source_book_url, source_book_name, source_author_name,
     match_confidence, is_primary_source, binding_status, create_time, update_time, deleted)
VALUES
    (90010201, 90010101, 90010001, 'https://www.69shuba.com/book/48214.htm',
     '玄鉴仙族', '一觉睡到自然醒', 100, 1, 'active', NOW(), NOW(), 0),
    (90010202, 90010102, 90010001, 'https://www.69shuba.com/book/38912.htm',
     '万相之王', '天蚕土豆', 100, 1, 'active', NOW(), NOW(), 0)
AS new
ON DUPLICATE KEY UPDATE
    source_book_name = new.source_book_name,
    binding_status = new.binding_status,
    update_time = NOW(),
    deleted = 0;

-- ── 4. 样例章节（作品「玄鉴仙族」前 3 章） ──────────────────────
INSERT INTO reading_chapter
    (id, work_id, chapter_title, chapter_index, volume_name, is_vip_chapter,
     chapter_status, create_time, update_time, deleted)
VALUES
    (90010301, 90010101, '第一章 玄鉴', 0, '第一卷', 0, 'active', NOW(), NOW(), 0),
    (90010302, 90010101, '第二章 灵石', 1, '第一卷', 0, 'active', NOW(), NOW(), 0),
    (90010303, 90010101, '第三章 试炼', 2, '第一卷', 0, 'active', NOW(), NOW(), 0)
AS new
ON DUPLICATE KEY UPDATE
    chapter_title = new.chapter_title,
    chapter_status = new.chapter_status,
    update_time = NOW(),
    deleted = 0;

-- ── 5. 章节-来源绑定（正文 URL 为内部源引用） ────────────────────
INSERT INTO reading_chapter_source_binding
    (id, work_id, source_id, chapter_id, source_chapter_url, source_chapter_title,
     source_chapter_order, binding_confidence, create_time, update_time, deleted)
VALUES
    (90010401, 90010101, 90010001, 90010301,
     'https://www.69shuba.com/txt/48214/38200001', '第一章 玄鉴', 0, 100, NOW(), NOW(), 0),
    (90010402, 90010101, 90010001, 90010302,
     'https://www.69shuba.com/txt/48214/38200002', '第二章 灵石', 1, 100, NOW(), NOW(), 0),
    (90010403, 90010101, 90010001, 90010303,
     'https://www.69shuba.com/txt/48214/38200003', '第三章 试炼', 2, 100, NOW(), NOW(), 0)
AS new
ON DUPLICATE KEY UPDATE
    source_chapter_title = new.source_chapter_title,
    source_chapter_order = new.source_chapter_order,
    update_time = NOW(),
    deleted = 0;

-- ── 6. 正文快照（直接落 sanitized，读侧无需回源即可返回正文） ────
INSERT INTO reading_chapter_content_snapshot
    (id, chapter_id, source_id, compiled_rule_id, raw_content, normalized_content,
     sanitized_content, content_hash, content_status, sanitization_pipeline_version,
     fetched_at, processed_at, create_time, update_time, deleted)
VALUES
    (90010501, 90010301, 90010001, NULL,
     '　　玄鉴悬于识海之上,少年缓缓睁开双眼。\n　　这是他穿越到这方仙族林立的世界的第一天。',
     '玄鉴悬于识海之上,少年缓缓睁开双眼。\n这是他穿越到这方仙族林立的世界的第一天。',
     '玄鉴悬于识海之上,少年缓缓睁开双眼。\n这是他穿越到这方仙族林立的世界的第一天。',
     REPEAT('a', 64), 'sanitized', 'v1', NOW(), NOW(), NOW(), NOW(), 0),
    (90010502, 90010302, 90010001, NULL,
     '　　一枚灵石静静躺在掌心,散发着温润的光泽。\n　　少年握紧拳头,眼中闪过一丝决然。',
     '一枚灵石静静躺在掌心,散发着温润的光泽。\n少年握紧拳头,眼中闪过一丝决然。',
     '一枚灵石静静躺在掌心,散发着温润的光泽。\n少年握紧拳头,眼中闪过一丝决然。',
     REPEAT('b', 64), 'sanitized', 'v1', NOW(), NOW(), NOW(), NOW(), 0),
    (90010503, 90010303, 90010001, NULL,
     '　　试炼之地风声呼啸,少年踏入其中。\n　　真正的修行,从这一刻才刚刚开始。',
     '试炼之地风声呼啸,少年踏入其中。\n真正的修行,从这一刻才刚刚开始。',
     '试炼之地风声呼啸,少年踏入其中。\n真正的修行,从这一刻才刚刚开始。',
     REPEAT('c', 64), 'sanitized', 'v1', NOW(), NOW(), NOW(), NOW(), 0)
AS new
ON DUPLICATE KEY UPDATE
    raw_content = new.raw_content,
    normalized_content = new.normalized_content,
    sanitized_content = new.sanitized_content,
    content_status = new.content_status,
    update_time = NOW(),
    deleted = 0;
