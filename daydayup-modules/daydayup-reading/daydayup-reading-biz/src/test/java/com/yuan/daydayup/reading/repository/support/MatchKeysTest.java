package com.yuan.daydayup.reading.repository.support;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link MatchKeys} 单测：保守归并键（吸收大小写/全半角/空白/标点，不做别名/模糊）。
 */
class MatchKeysTest {

    @Test
    void normalizeAbsorbsCaseWidthSpacePunct() {
        assertEquals(MatchKeys.of("凡人修仙传", "忘语"), MatchKeys.of(" 凡人修仙传 ", "忘语"));
        assertEquals(MatchKeys.of("Rebirth", "Author"), MatchKeys.of("rebirth", "author"));
        // 全角标点/括号差异被吸收
        assertEquals(MatchKeys.of("斗破苍穹（精校版）", "天蚕土豆"),
                MatchKeys.of("斗破苍穹(精校版)", "天蚕土豆"));
    }

    @Test
    void differentTitleOrAuthorNotMerged() {
        assertNotEquals(MatchKeys.of("凡人修仙传", "忘语"), MatchKeys.of("凡人修仙传", "耳根"));
        assertNotEquals(MatchKeys.of("仙逆", "耳根"), MatchKeys.of("仙逆传", "耳根"));
    }

    @Test
    void nullAuthorStable() {
        assertEquals(MatchKeys.of("书名", null), MatchKeys.of("书名", ""));
    }

    @Test
    void pickTakesFirstNonBlank() {
        Map<String, String> record = Map.of("name", "  ", "bookName", "凡人修仙传");
        assertEquals("凡人修仙传", MatchKeys.pick(record, "name", "bookName", "title"));
        assertNull(MatchKeys.pick(record, "missing"));
    }
}
