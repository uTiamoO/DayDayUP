package com.yuan.daydayup.reading.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.reading.repository.entity.Work;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统一作品 Mapper
 */
@Mapper
public interface WorkMapper extends BaseMapper<Work> {

    /** 按保守归并键取最早一条（find-or-create 用），逻辑删除由 MyBatis-Plus 自动过滤 */
    @Select("SELECT * FROM reading_work WHERE match_key = #{matchKey} AND deleted = 0 ORDER BY id ASC LIMIT 1")
    Work selectByMatchKey(@Param("matchKey") String matchKey);

    /** 阅读 API 统一作品分页查询（可按关键词、分类、状态、来源过滤）。 */
    @Select("<script>"
            + "SELECT w.* FROM reading_work w "
            + "WHERE w.deleted = 0 "
            + "<if test='keyword != null and keyword != &quot;&quot;'>"
            + "AND (w.title LIKE CONCAT('%', #{keyword}, '%') OR w.author_name LIKE CONCAT('%', #{keyword}, '%')) "
            + "</if>"
            + "<if test='category != null and category != &quot;&quot;'>"
            + "AND w.category_name = #{category} "
            + "</if>"
            + "<if test='status != null and status != &quot;&quot;'>"
            + "AND w.completion_status = #{status} "
            + "</if>"
            + "<if test='sourceId != null'>"
            + "AND EXISTS (SELECT 1 FROM reading_work_source_binding b "
            + "WHERE b.work_id = w.id AND b.source_id = #{sourceId} AND b.deleted = 0 AND b.binding_status = 'active') "
            + "</if>"
            + "<choose>"
            + "<when test='sort == &quot;latest&quot;'>ORDER BY w.latest_chapter_updated_at DESC, w.update_time DESC, w.id DESC</when>"
            + "<otherwise>ORDER BY w.update_time DESC, w.id DESC</otherwise>"
            + "</choose>"
            + "</script>")
    Page<Work> selectReadingPage(Page<Work> page,
                                 @Param("keyword") String keyword,
                                 @Param("category") String category,
                                 @Param("status") String status,
                                 @Param("sourceId") Long sourceId,
                                 @Param("sort") String sort);

    /** 分类筛选元数据。 */
    @Select("SELECT category_name AS name, COUNT(1) AS count FROM reading_work "
            + "WHERE deleted = 0 AND category_name IS NOT NULL AND category_name <> '' "
            + "GROUP BY category_name ORDER BY count DESC, category_name ASC LIMIT #{limit}")
    List<Map<String, Object>> selectCategoryCounts(@Param("limit") int limit);

    /** 完结状态筛选元数据。 */
    @Select("SELECT completion_status AS name, COUNT(1) AS count FROM reading_work "
            + "WHERE deleted = 0 AND completion_status IS NOT NULL AND completion_status <> '' "
            + "GROUP BY completion_status ORDER BY count DESC, completion_status ASC")
    List<Map<String, Object>> selectCompletionStatusCounts();
}
