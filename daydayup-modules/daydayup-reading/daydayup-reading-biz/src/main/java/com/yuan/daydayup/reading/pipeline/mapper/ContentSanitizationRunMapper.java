package com.yuan.daydayup.reading.pipeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuan.daydayup.reading.pipeline.entity.ContentSanitizationRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 净化运行记录 Mapper
 */
@Mapper
public interface ContentSanitizationRunMapper extends BaseMapper<ContentSanitizationRun> {

    /** 查询某正文快照最近一次净化运行记录。 */
    @Select("SELECT * FROM reading_content_sanitization_run "
            + "WHERE content_snapshot_id = #{contentSnapshotId} AND deleted = 0 "
            + "ORDER BY run_at DESC, id DESC LIMIT 1")
    ContentSanitizationRun selectLatestBySnapshotId(@Param("contentSnapshotId") Long contentSnapshotId);
}
