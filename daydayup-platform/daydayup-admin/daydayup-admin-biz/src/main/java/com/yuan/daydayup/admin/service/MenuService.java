package com.yuan.daydayup.admin.service;

import com.yuan.daydayup.admin.dto.MenuCreateDTO;
import com.yuan.daydayup.admin.dto.MenuPageQueryDTO;
import com.yuan.daydayup.admin.dto.MenuStatusDTO;
import com.yuan.daydayup.admin.dto.MenuUpdateDTO;
import com.yuan.daydayup.admin.vo.MenuTreeVO;
import com.yuan.daydayup.admin.vo.MenuVO;
import com.yuan.daydayup.common.mybatis.service.BaseCrudService;
import com.yuan.daydayup.common.mybatis.service.DeletableCrudService;

import java.util.List;
import java.util.Set;

public interface MenuService extends BaseCrudService<Long, MenuCreateDTO, MenuUpdateDTO, MenuVO, MenuPageQueryDTO, MenuStatusDTO>,
        DeletableCrudService<Long> {

    /** 全量菜单树（管理用，含停用/隐藏项）。 */
    List<MenuTreeVO> tree();

    /** 当前用户有权访问的菜单树（启用+可见，按权限码过滤，排除按钮）。 */
    List<MenuTreeVO> currentUserMenus(Set<String> authorities);
}
