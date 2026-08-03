package com.yuan.daydayup.reading.db;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 初始化 SQL 基线测试。
 */
class InitSqlTest {

    @Test
    void initSqlContainsCoreReadingTablesAndIndexes() throws Exception {
        String sql;
        try (var in = getClass().getClassLoader().getResourceAsStream("db/init.sql")) {
            assertNotNull(in, "db/init.sql must exist");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String table : new String[]{
                "reading_source_definition",
                "reading_source_compiled_rule",
                "reading_work",
                "reading_work_source_binding",
                "reading_chapter",
                "reading_chapter_source_binding",
                "reading_chapter_content_snapshot",
                "reading_content_sanitization_run"
        }) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table), table + " DDL missing");
        }
        assertTrue(sql.contains("uk_reading_source_definition_url"));
        assertTrue(sql.contains("uk_reading_work_source_book"));
        assertTrue(sql.contains("uk_reading_chapter_content_snapshot_chapter_source"));
        assertTrue(sql.contains("create_time"));
        assertTrue(sql.contains("update_time"));
        assertTrue(sql.contains("deleted"));
    }
}
