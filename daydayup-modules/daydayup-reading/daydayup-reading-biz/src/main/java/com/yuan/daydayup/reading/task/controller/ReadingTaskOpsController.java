package com.yuan.daydayup.reading.task.controller;

import com.yuan.daydayup.common.core.result.R;
import com.yuan.daydayup.reading.api.vo.ReadingPageVO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskDrainVO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskPageQueryDTO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskSubmitDTO;
import com.yuan.daydayup.reading.api.vo.ReadingTaskVO;
import com.yuan.daydayup.reading.task.service.ReadingTaskService;
import com.yuan.daydayup.reading.task.service.ReadingTaskWorker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阅读任务运营接口（仅内网可达）。
 *
 * <p>路径前缀 {@code /api/v1/internal/**}，网关不配置对外路由。</p>
 */
@Tag(name = "阅读任务运营", description = "任务提交 / 查询 / 取消 / worker drain（内网）")
@RestController
@RequestMapping("/api/v1/internal/ops/tasks")
public class ReadingTaskOpsController {

    private final ReadingTaskService taskService;
    private final ReadingTaskWorker taskWorker;

    public ReadingTaskOpsController(ReadingTaskService taskService, ReadingTaskWorker taskWorker) {
        this.taskService = taskService;
        this.taskWorker = taskWorker;
    }

    @Operation(summary = "提交阅读任务", description = "按 taskType + bizKey 对 pending/running 任务幂等提交")
    @PostMapping("/submit")
    public R<ReadingTaskVO> submit(@RequestBody ReadingTaskSubmitDTO request) {
        return R.ok(taskService.submit(request));
    }

    @Operation(summary = "查询阅读任务详情")
    @GetMapping("/{taskId}")
    public R<ReadingTaskVO> get(@PathVariable("taskId") Long taskId) {
        return R.ok(taskService.get(taskId));
    }

    @Operation(summary = "分页查询阅读任务")
    @GetMapping("/page")
    public R<ReadingPageVO<ReadingTaskVO>> page(@ModelAttribute ReadingTaskPageQueryDTO query) {
        return R.ok(taskService.page(query));
    }

    @Operation(summary = "取消 pending 阅读任务")
    @PostMapping("/{taskId}/cancel")
    public R<ReadingTaskVO> cancel(@PathVariable("taskId") Long taskId) {
        return R.ok(taskService.cancel(taskId));
    }

    @Operation(summary = "手动 drain 到期任务", description = "内网手动触发一批任务执行；自动 worker 默认关闭")
    @PostMapping("/drain")
    public R<ReadingTaskDrainVO> drain(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        return R.ok(taskWorker.drain(limit));
    }
}
