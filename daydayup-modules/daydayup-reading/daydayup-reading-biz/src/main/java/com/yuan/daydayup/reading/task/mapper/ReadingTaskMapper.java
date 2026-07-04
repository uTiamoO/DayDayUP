package com.yuan.daydayup.reading.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuan.daydayup.reading.task.entity.ReadingTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 阅读任务 Mapper。
 */
@Mapper
public interface ReadingTaskMapper extends BaseMapper<ReadingTask> {

    @Select("SELECT * FROM reading_task WHERE task_type = #{taskType} AND biz_key = #{bizKey} "
            + "AND task_status IN ('pending','running') AND deleted = 0 ORDER BY id ASC LIMIT 1")
    ReadingTask selectActive(@Param("taskType") String taskType, @Param("bizKey") String bizKey);

    @Select("<script>"
            + "SELECT * FROM reading_task WHERE deleted = 0 "
            + "<if test='taskType != null and taskType != &quot;&quot;'>AND task_type = #{taskType} </if>"
            + "<if test='status != null and status != &quot;&quot;'>AND task_status = #{status} </if>"
            + "ORDER BY create_time DESC, id DESC"
            + "</script>")
    Page<ReadingTask> selectTaskPage(Page<ReadingTask> page,
                                     @Param("taskType") String taskType,
                                     @Param("status") String status);

    @Select("SELECT * FROM reading_task WHERE deleted = 0 AND ("
            + "(task_status = 'pending' AND (next_run_at IS NULL OR next_run_at <= #{now})) "
            + "OR (task_status = 'running' AND locked_at IS NOT NULL AND locked_at <= #{lockExpiredBefore})"
            + ") ORDER BY COALESCE(next_run_at, create_time) ASC, id ASC LIMIT #{limit}")
    List<ReadingTask> selectAcquireCandidates(@Param("now") LocalDateTime now,
                                              @Param("lockExpiredBefore") LocalDateTime lockExpiredBefore,
                                              @Param("limit") int limit);

    @Update("UPDATE reading_task SET task_status = 'running', locked_by = #{workerId}, locked_at = #{now}, "
            + "started_at = #{now}, finished_at = NULL, update_time = #{now} "
            + "WHERE id = #{id} AND deleted = 0 AND ("
            + "(task_status = 'pending' AND (next_run_at IS NULL OR next_run_at <= #{now})) "
            + "OR (task_status = 'running' AND locked_at IS NOT NULL AND locked_at <= #{lockExpiredBefore})"
            + ")")
    int tryAcquire(@Param("id") Long id,
                   @Param("workerId") String workerId,
                   @Param("now") LocalDateTime now,
                   @Param("lockExpiredBefore") LocalDateTime lockExpiredBefore);

    @Update("UPDATE reading_task SET task_status = #{status}, locked_by = NULL, locked_at = NULL, "
            + "finished_at = #{now}, error_message = NULL, update_time = #{now} "
            + "WHERE id = #{id} AND deleted = 0")
    int markFinished(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("now") LocalDateTime now);

    @Update("UPDATE reading_task SET task_status = #{status}, retry_count = #{retryCount}, next_run_at = #{nextRunAt}, "
            + "locked_by = NULL, locked_at = NULL, error_message = #{errorMessage}, "
            + "finished_at = #{finishedAt}, update_time = #{now} WHERE id = #{id} AND deleted = 0")
    int markFailure(@Param("id") Long id,
                    @Param("status") String status,
                    @Param("retryCount") int retryCount,
                    @Param("nextRunAt") LocalDateTime nextRunAt,
                    @Param("errorMessage") String errorMessage,
                    @Param("finishedAt") LocalDateTime finishedAt,
                    @Param("now") LocalDateTime now);

    @Update("UPDATE reading_task SET task_status = 'cancelled', finished_at = #{now}, update_time = #{now} "
            + "WHERE id = #{id} AND task_status = 'pending' AND deleted = 0")
    int cancelPending(@Param("id") Long id, @Param("now") LocalDateTime now);
}
