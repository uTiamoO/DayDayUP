package com.yuan.daydayup.reading.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.reading.repository.entity.ChapterSourceBinding;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 章节-书源绑定 Mapper
 */
@Mapper
public interface ChapterSourceBindingMapper extends BaseMapper<ChapterSourceBinding> {

    /** 物理删除某作品某来源的全部章节绑定（目录重建先清后建） */
    @Delete("DELETE FROM reading_chapter_source_binding WHERE work_id = #{workId} AND source_id = #{sourceId}")
    int deleteByWorkAndSource(@Param("workId") Long workId, @Param("sourceId") Long sourceId);

    /** 按 (chapterId, sourceId) 查绑定，取来源章节正文 URL */
    @Select("SELECT * FROM reading_chapter_source_binding "
            + "WHERE chapter_id = #{chapterId} AND source_id = #{sourceId} AND deleted = 0 LIMIT 1")
    ChapterSourceBinding selectByChapterAndSource(@Param("chapterId") Long chapterId,
                                                  @Param("sourceId") Long sourceId);

    /** 查询某作品某来源的全部章节绑定，用于章节列表标记来源可用性。 */
    @Select("SELECT * FROM reading_chapter_source_binding "
            + "WHERE work_id = #{workId} AND source_id = #{sourceId} AND deleted = 0")
    List<ChapterSourceBinding> selectByWorkAndSource(@Param("workId") Long workId,
                                                     @Param("sourceId") Long sourceId);
}
