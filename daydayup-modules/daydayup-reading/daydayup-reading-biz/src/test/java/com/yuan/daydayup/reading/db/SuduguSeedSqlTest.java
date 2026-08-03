package com.yuan.daydayup.reading.db;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuduguSeedSqlTest {

    @Test
    void registersSuduguAsIdempotentNativeSource() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db/data-sudugu.sql")) {
            assertNotNull(input, "速读谷 native seed SQL 缺失");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("'https://www.sudugu.org'"));
            assertTrue(sql.contains("'native'"));
            assertTrue(sql.contains("'sudugu'"));
            assertTrue(sql.contains("ON DUPLICATE KEY UPDATE"));
        }
    }
}
