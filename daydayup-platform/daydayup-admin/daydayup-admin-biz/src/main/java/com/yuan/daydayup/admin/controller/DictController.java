package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.dto.DictCreateDTO;
import com.yuan.daydayup.admin.dto.DictPageQueryDTO;
import com.yuan.daydayup.admin.dto.DictStatusDTO;
import com.yuan.daydayup.admin.dto.DictUpdateDTO;
import com.yuan.daydayup.admin.service.DictService;
import com.yuan.daydayup.admin.vo.DictVO;
import com.yuan.daydayup.common.core.page.PageResult;
import com.yuan.daydayup.common.core.result.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/page")
    @PreAuthorize("hasPermission(null, 'admin:dict:list')")
    public R<PageResult<DictVO>> page(DictPageQueryDTO query) {
        return R.ok(dictService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:dict:detail')")
    public R<DictVO> detail(@PathVariable Long id) {
        return R.ok(dictService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'admin:dict:create')")
    public R<DictVO> create(@Valid @RequestBody DictCreateDTO dto) {
        return R.ok(dictService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:dict:update')")
    public R<DictVO> update(@PathVariable Long id, @Valid @RequestBody DictUpdateDTO dto) {
        return R.ok(dictService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'admin:dict:status')")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody DictStatusDTO dto) {
        dictService.changeStatus(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:dict:delete')")
    public R<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return R.ok();
    }
}
