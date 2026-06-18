package com.yuan.daydayup.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuan.daydayup.admin.dto.MenuCreateDTO;
import com.yuan.daydayup.admin.dto.MenuPageQueryDTO;
import com.yuan.daydayup.admin.dto.MenuStatusDTO;
import com.yuan.daydayup.admin.dto.MenuUpdateDTO;
import com.yuan.daydayup.admin.entity.SysMenu;
import com.yuan.daydayup.admin.mapper.SysMenuMapper;
import com.yuan.daydayup.admin.service.MenuService;
import com.yuan.daydayup.admin.vo.MenuTreeVO;
import com.yuan.daydayup.admin.vo.MenuVO;
import com.yuan.daydayup.common.core.enums.ErrorCode;
import com.yuan.daydayup.common.core.exception.BizException;
import com.yuan.daydayup.common.mybatis.service.AbstractCrudService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MenuServiceImpl
        extends AbstractCrudService<SysMenuMapper, SysMenu, Long, MenuCreateDTO, MenuUpdateDTO, MenuVO, MenuPageQueryDTO, MenuStatusDTO>
        implements MenuService {

    public MenuServiceImpl(SysMenuMapper mapper) {
        super(mapper);
    }

    @Override
    public List<MenuTreeVO> tree() {
        List<SysMenu> all = mapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
        return buildTree(all);
    }

    @Override
    public List<MenuTreeVO> currentUserMenus(Set<String> authorities) {
        List<SysMenu> menus = mapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getVisible, 1)
                .ne(SysMenu::getType, "BUTTON")
                .orderByAsc(SysMenu::getSort));
        return pruneByAuthority(buildTree(menus), authorities);
    }

    private List<MenuTreeVO> buildTree(List<SysMenu> menus) {
        Map<Long, MenuTreeVO> byId = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            byId.put(menu.getId(), toTreeVO(menu));
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            MenuTreeVO node = byId.get(menu.getId());
            MenuTreeVO parent = menu.getParentId() == null ? null : byId.get(menu.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 按权限裁剪菜单树：保留有权访问的菜单；DIRECTORY 仅在仍有可见子节点时保留。
     */
    private List<MenuTreeVO> pruneByAuthority(List<MenuTreeVO> nodes, Set<String> authorities) {
        List<MenuTreeVO> kept = new ArrayList<>();
        for (MenuTreeVO node : nodes) {
            node.setChildren(pruneByAuthority(node.getChildren(), authorities));
            if (!permitted(authorities, node.getPermissionCode())) {
                continue;
            }
            if ("DIRECTORY".equals(node.getType()) && node.getChildren().isEmpty()) {
                continue;
            }
            kept.add(node);
        }
        return kept;
    }

    private boolean permitted(Set<String> authorities, String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            return true;
        }
        if (authorities == null) {
            return false;
        }
        for (String authority : authorities) {
            if (authority.equals(permissionCode)) {
                return true;
            }
            if (authority.endsWith(":*")
                    && permissionCode.startsWith(authority.substring(0, authority.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    private MenuTreeVO toTreeVO(SysMenu menu) {
        return MenuTreeVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .code(menu.getCode())
                .name(menu.getName())
                .path(menu.getPath())
                .component(menu.getComponent())
                .icon(menu.getIcon())
                .type(menu.getType())
                .permissionCode(menu.getPermissionCode())
                .sort(menu.getSort())
                .visible(menu.getVisible())
                .status(menu.getStatus())
                .children(new ArrayList<>())
                .build();
    }

    @Override
    protected LambdaQueryWrapper<SysMenu> buildPageQueryWrapper(MenuPageQueryDTO query) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), SysMenu::getName, query.getName())
               .eq(StringUtils.hasText(query.getType()), SysMenu::getType, query.getType())
               .eq(query.getStatus() != null, SysMenu::getStatus, query.getStatus())
               .orderByAsc(SysMenu::getSort)
               .orderByDesc(SysMenu::getCreateTime);
        return wrapper;
    }

    @Override
    protected SysMenu toEntity(MenuCreateDTO dto) {
        SysMenu entity = new SysMenu();
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setParentId(dto.getParentId());
        entity.setPath(dto.getPath());
        entity.setComponent(dto.getComponent());
        entity.setIcon(dto.getIcon());
        entity.setType(dto.getType());
        entity.setPermissionCode(dto.getPermissionCode());
        entity.setSort(dto.getSort());
        entity.setVisible(dto.getVisible());
        entity.setStatus(1);
        return entity;
    }

    @Override
    protected void updateEntity(SysMenu entity, MenuUpdateDTO dto) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setParentId(dto.getParentId());
        entity.setPath(dto.getPath());
        entity.setComponent(dto.getComponent());
        entity.setIcon(dto.getIcon());
        entity.setType(dto.getType());
        entity.setPermissionCode(dto.getPermissionCode());
        entity.setSort(dto.getSort());
        entity.setVisible(dto.getVisible());
    }

    @Override
    protected MenuVO toVO(SysMenu entity) {
        return MenuVO.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .code(entity.getCode())
                .name(entity.getName())
                .path(entity.getPath())
                .component(entity.getComponent())
                .icon(entity.getIcon())
                .type(entity.getType())
                .permissionCode(entity.getPermissionCode())
                .sort(entity.getSort())
                .visible(entity.getVisible())
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    @Override
    protected void updateStatus(SysMenu entity, MenuStatusDTO statusDTO) {
        entity.setStatus(statusDTO.getStatus());
    }

    @Override
    protected void validateBeforeCreate(MenuCreateDTO dto) {
        checkCodeUnique(dto.getCode(), null);
    }

    @Override
    protected void validateBeforeUpdate(Long id, MenuUpdateDTO dto) {
        checkCodeUnique(dto.getCode(), id);
    }

    @Override
    public void delete(Long id) {
        Long childCount = mapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR, "存在子菜单，不允许删除");
        }
        mapper.deleteById(id);
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        if (mapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.DATA_ALREADY_EXISTS, "菜单编码已存在");
        }
    }
}
