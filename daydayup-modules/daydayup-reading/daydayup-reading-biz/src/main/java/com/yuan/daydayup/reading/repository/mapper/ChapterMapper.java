package com.yuan.daydayup.reading.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.reading.repository.entity.Chapter;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 统一章节 Mapper
 */
@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {

    /** 物理删除某作品的全部统一章节（目录重建：主来源目录变更时先清后建，避免 uk_work_index 冲突） */
    @Delete("DELETE FROM reading_chapter WHERE work_id = #{workId}")
    int deleteByWorkId(@Param("workId") Long workId);

    /** 分页查询作品章节，按统一章节序升序。 */
    @Select("SELECT * FROM reading_chapter "
            + "WHERE work_id = #{workId} AND deleted = 0 "
            + "ORDER BY chapter_index ASC, id ASC")
    Page<Chapter> selectPageByWorkId(Page<Chapter> page, @Param("workId") Long workId);

    /** 查询作品全部统一章节，用于副来源目录惰性对齐。 */
    @Select("SELECT * FROM reading_chapter "
            + "WHERE work_id = #{workId} AND deleted = 0 "
            + "ORDER BY chapter_index ASC, id ASC")
    List<Chapter> selectByWorkId(@Param("workId") Long workId);

    /** 查询作品最新章节（以章节序最大为准）。 */
    @Select("SELECT * FROM reading_chapter "
            + "WHERE work_id = #{workId} AND deleted = 0 "
            + "ORDER BY chapter_index DESC, id DESC LIMIT 1")
    Chapter selectLatestByWorkId(@Param("workId") Long workId);
}
