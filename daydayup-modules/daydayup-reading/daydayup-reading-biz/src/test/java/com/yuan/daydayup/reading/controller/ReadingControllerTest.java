package com.yuan.daydayup.reading.controller;

import com.yuan.daydayup.reading.api.vo.ReadingContentVO;
import com.yuan.daydayup.reading.api.vo.ReadingPageVO;
import com.yuan.daydayup.reading.api.vo.ReadingWorkDetailVO;
import com.yuan.daydayup.reading.service.ReadingContentService;
import com.yuan.daydayup.reading.service.ReadingQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReadingControllerTest {

    private ReadingQueryService readingQueryService;
    private ReadingContentService readingContentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        readingQueryService = mock(ReadingQueryService.class);
        readingContentService = mock(ReadingContentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ReadingController(readingQueryService, readingContentService)).build();
    }

    @Test
    void detailDefaultsToCacheFirst() throws Exception {
        when(readingQueryService.detail(100L, null, "cache-first"))
                .thenReturn(new ReadingWorkDetailVO());

        mockMvc.perform(get("/api/v1/reading/works/100"))
                .andExpect(status().isOk());

        verify(readingQueryService).detail(100L, null, "cache-first");
    }

    @Test
    void chaptersDefaultToCacheFirst() throws Exception {
        when(readingQueryService.chapters(100L, null, "cache-first", 1, 50))
                .thenReturn(new ReadingPageVO<>());

        mockMvc.perform(get("/api/v1/reading/works/100/chapters"))
                .andExpect(status().isOk());

        verify(readingQueryService).chapters(100L, null, "cache-first", 1, 50);
    }

    @Test
    void contentDefaultsToSanitizedCacheFirst() throws Exception {
        when(readingContentService.content(200L, 10L, "sanitized", "cache-first"))
                .thenReturn(new ReadingContentVO());

        mockMvc.perform(get("/api/v1/reading/chapters/200/content")
                        .param("sourceId", "10"))
                .andExpect(status().isOk());

        verify(readingContentService).content(200L, 10L, "sanitized", "cache-first");
    }
}
