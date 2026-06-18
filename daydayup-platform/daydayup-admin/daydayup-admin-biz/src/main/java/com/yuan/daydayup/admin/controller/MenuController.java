package com.yuan.daydayup.admin.controller;

import com.yuan.daydayup.admin.dto.MenuCreateDTO;
import com.yuan.daydayup.admin.dto.MenuPageQueryDTO;
import com.yuan.daydayup.admin.dto.MenuStatusDTO;
import com.yuan.daydayup.admin.dto.MenuUpdateDTO;
import com.yuan.daydayup.admin.service.MenuService;
import com.yuan.daydayup.admin.vo.MenuTreeVO;
import com.yuan.daydayup.admin.vo.MenuVO;
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

import java.util.List;

@RestController
@RequestMapping("/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/page")
    @PreAuthorize("hasPermission(null, 'admin:menu:list')")
    public R<PageResult<MenuVO>> page(MenuPageQueryDTO query) {
        return R.ok(menuService.page(query));
    }

    @GetMapping("/tree")
    @PreAuthorize("hasPermission(null, 'admin:menu:list')")
    public R<List<MenuTreeVO>> tree() {
        return R.ok(menuService.tree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:menu:detail')")
    public R<MenuVO> detail(@PathVariable Long id) {
        return R.ok(menuService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'admin:menu:create')")
    public R<MenuVO> create(@Valid @RequestBody MenuCreateDTO dto) {
        return R.ok(menuService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:menu:update')")
    public R<MenuVO> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateDTO dto) {
        return R.ok(menuService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission(null, 'admin:menu:status')")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody MenuStatusDTO dto) {
        menuService.changeStatus(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'admin:menu:delete')")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}
