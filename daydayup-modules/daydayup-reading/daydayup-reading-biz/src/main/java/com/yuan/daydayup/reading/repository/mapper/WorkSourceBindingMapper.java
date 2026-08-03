package com.yuan.daydayup.reading.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.reading.repository.entity.WorkSourceBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 作品-书源绑定 Mapper
 */
@Mapper
public interface WorkSourceBindingMapper extends BaseMapper<WorkSourceBinding> {

    /** 按该源内幂等键 (sourceId, sourceBookUrl) 查绑定 */
    @Select("SELECT * FROM reading_work_source_binding "
            + "WHERE source_id = #{sourceId} AND source_book_url = #{sourceBookUrl} AND deleted = 0 LIMIT 1")
    WorkSourceBinding selectBySourceBook(@Param("sourceId") Long sourceId,
                                         @Param("sourceBookUrl") String sourceBookUrl);

    /** 按 (workId, sourceId) 查绑定（目录同步定位来源书籍 URL 与主来源标记） */
    @Select("SELECT * FROM reading_work_source_binding "
            + "WHERE work_id = #{workId} AND source_id = #{sourceId} AND deleted = 0 LIMIT 1")
    WorkSourceBinding selectByWorkAndSource(@Param("workId") Long workId, @Param("sourceId") Long sourceId);

    /** 查询作品下所有 active 来源绑定。 */
    @Select("SELECT * FROM reading_work_source_binding "
            + "WHERE work_id = #{workId} AND deleted = 0 AND binding_status = 'active' "
            + "ORDER BY is_primary_source DESC, id ASC")
    List<WorkSourceBinding> selectActiveByWorkId(@Param("workId") Long workId);

    /** 分页查询作品下 active 来源绑定，避免来源过多时一次性加载。 */
    @Select("SELECT * FROM reading_work_source_binding "
            + "WHERE work_id = #{workId} AND deleted = 0 AND binding_status = 'active' "
            + "ORDER BY is_primary_source DESC, id ASC")
    Page<WorkSourceBinding> selectActivePageByWorkId(Page<WorkSourceBinding> page, @Param("workId") Long workId);

    /** 批量查询书源绑定关联的书源定义，避免逐条 selectById。 */
    @Select("<script>"
            + "SELECT b.*, s.name AS sourceName, s.site_name AS siteName, s.status AS sourceStatus, s.priority AS sourcePriority "
            + "FROM reading_work_source_binding b "
            + "LEFT JOIN reading_source_definition s ON s.id = b.source_id AND s.deleted = 0 "
            + "WHERE b.id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<java.util.Map<String, Object>> selectSourceSummariesByBindingIds(@Param("ids") List<Long> ids);

    /** 统计作品来源数。 */
    @Select("SELECT COUNT(1) FROM reading_work_source_binding "
            + "WHERE work_id = #{workId} AND deleted = 0 AND binding_status = 'active'")
    int countActiveByWorkId(@Param("workId") Long workId);

    /** 查询作品主来源绑定。 */
    @Select("SELECT * FROM reading_work_source_binding "
            + "WHERE work_id = #{workId} AND deleted = 0 AND binding_status = 'active' "
            + "ORDER BY is_primary_source DESC, id ASC LIMIT 1")
    WorkSourceBinding selectPrimaryByWorkId(@Param("workId") Long workId);
}
