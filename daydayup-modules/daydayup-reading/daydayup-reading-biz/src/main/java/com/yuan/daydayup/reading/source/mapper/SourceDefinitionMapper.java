package com.yuan.daydayup.reading.source.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.reading.source.entity.SourceDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 书源定义 Mapper
 */
@Mapper
public interface SourceDefinitionMapper extends BaseMapper<SourceDefinition> {

    /**
     * 按幂等键查询，且不过滤逻辑删除（O9：软删同 URL 书源重导入需走「恢复」而非 insert，
     * 否则撞 uk_book_source_url 唯一键）。
     */
    @Select("SELECT * FROM reading_source_definition "
            + "WHERE (book_source_url = #{url} OR book_source_url = CONCAT(#{url}, '/')) LIMIT 1")
    SourceDefinition selectByUrlIncludeDeleted(@Param("url") String url);

    /** 恢复软删记录（deleted 置回 0） */
    @Update("UPDATE reading_source_definition SET deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);

    /** 查询启用书源列表，按优先级降序。 */
    @Select("SELECT * FROM reading_source_definition WHERE deleted = 0 AND status = 1 "
            + "ORDER BY priority DESC, id ASC LIMIT #{limit}")
    List<SourceDefinition> selectEnabledSources(@Param("limit") int limit);
}
