-- DayDayUP Reading - sudugu.org native source registration.
INSERT INTO reading_source_definition
    (id, name, site_name, book_source_url, book_source_type, origin_type, origin_path,
     status, priority, tags, compile_grade, fingerprint, imported_at, raw_content,
     create_time, update_time, deleted)
VALUES
    (90020001, '速读谷', '自研书源', 'https://www.sudugu.org', 0, 'native', 'native:sudugu',
     1, 90, 'sudugu', 'full', REPEAT('0', 64), NOW(), '{"nativeParser":"sudugu"}',
     NOW(), NOW(), 0)
AS new
ON DUPLICATE KEY UPDATE
    name = new.name,
    site_name = new.site_name,
    origin_type = new.origin_type,
    origin_path = new.origin_path,
    status = new.status,
    priority = new.priority,
    tags = new.tags,
    compile_grade = new.compile_grade,
    raw_content = new.raw_content,
    update_time = NOW(),
    deleted = 0;
