package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.dto.OperLogPageQueryDTO;
import com.yuan.daydayup.admin.service.OperLogService;
import com.yuan.daydayup.admin.vo.OperLogVO;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oper-logs")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;

    @GetMapping("/page")
    @PreAuthorize("hasPermission(null, 'admin:oper-log:list')")
    public R<PageResult<OperLogVO>> page(OperLogPageQueryDTO query) {
        return R.ok(operLogService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:oper-log:detail')")
    public R<OperLogVO> detail(@PathVariable Long id) {
        return R.ok(operLogService.detail(id));
    }
}
