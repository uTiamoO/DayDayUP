package com.yuan.daydayup.reading.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.reading.repository.entity.ChapterContentSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 章节正文快照 Mapper
 */
@Mapper
public interface ChapterContentSnapshotMapper extends BaseMapper<ChapterContentSnapshot> {

    /** 按 (chapterId, sourceId) 查快照（幂等键） */
    @Select("SELECT * FROM reading_chapter_content_snapshot "
            + "WHERE chapter_id = #{chapterId} AND source_id = #{sourceId} AND deleted = 0 LIMIT 1")
    ChapterContentSnapshot selectByChapterAndSource(@Param("chapterId") Long chapterId,
                                                    @Param("sourceId") Long sourceId);
}
