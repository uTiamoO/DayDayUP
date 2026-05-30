package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.dto.DictItemCreateDTO;
import com.yuan.daydayup.admin.dto.DictItemPageQueryDTO;
import com.yuan.daydayup.admin.dto.DictItemStatusDTO;
import com.yuan.daydayup.admin.dto.DictItemUpdateDTO;
import com.yuan.daydayup.admin.service.DictItemService;
import com.yuan.daydayup.admin.vo.DictItemVO;
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
@RequestMapping("/dict-items")
@RequiredArgsConstructor
public class DictItemController {

    private final DictItemService dictItemService;

    @GetMapping("/page")
    @PreAuthorize("hasPermission(null, 'admin:dict-item:list')")
    public R<PageResult<DictItemVO>> page(DictItemPageQueryDTO query) {
        return R.ok(dictItemService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:dict-item:detail')")
    public R<DictItemVO> detail(@PathVariable Long id) {
        return R.ok(dictItemService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'admin:dict-item:create')")
    public R<DictItemVO> create(@Valid @RequestBody DictItemCreateDTO dto) {
        return R.ok(dictItemService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:dict-item:update')")
    public R<DictItemVO> update(@PathVariable Long id, @Valid @RequestBody DictItemUpdateDTO dto) {
        return R.ok(dictItemService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'admin:dict-item:status')")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody DictItemStatusDTO dto) {
        dictItemService.changeStatus(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:dict-item:delete')")
    public R<Void> delete(@PathVariable Long id) {
        dictItemService.delete(id);
        return R.ok();
    }
}
