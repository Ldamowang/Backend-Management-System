package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.service.CacheService;
import com.iflytek.admin.modules.system.dto.MenuFormDTO;
import com.iflytek.admin.modules.system.entity.SysMenu;
import com.iflytek.admin.modules.system.mapper.SysMenuMapper;
import com.iflytek.admin.modules.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper menuMapper;
    private final CacheService cacheService;

    @Override
    public List<Map<String, Object>> getMenuTree() {
        List<SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder));
        return buildTree(menus, 0L);
    }

    @Override
    public SysMenu getById(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public void create(MenuFormDTO dto) {
        SysMenu menu = new SysMenu();
        applyDTO(menu, dto);
        menuMapper.insert(menu);
        cacheService.deleteByPrefix(CacheConstants.MENU_USER_PREFIX);
        cacheService.deleteByPrefix(CacheConstants.PERM_USER_PREFIX);
    }

    @Override
    public void update(Long id, MenuFormDTO dto) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) throw new BusinessException(404, "菜单不存在");
        applyDTO(menu, dto);
        menuMapper.updateById(menu);
        cacheService.deleteByPrefix(CacheConstants.MENU_USER_PREFIX);
        cacheService.deleteByPrefix(CacheConstants.PERM_USER_PREFIX);
    }

    @Override
    public void delete(Long id) {
        Long count = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (count > 0) throw new BusinessException(400, "存在子菜单，无法删除");
        menuMapper.deleteById(id);
        cacheService.deleteByPrefix(CacheConstants.MENU_USER_PREFIX);
        cacheService.deleteByPrefix(CacheConstants.PERM_USER_PREFIX);
    }

    private void applyDTO(SysMenu menu, MenuFormDTO dto) {
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setIcon(dto.getIcon());
        menu.setSortOrder(dto.getSortOrder());
        menu.setPermission(dto.getPermission());
        menu.setVisible(dto.getVisible());
        menu.setStatus(dto.getStatus());
    }

    private List<Map<String, Object>> buildTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .map(m -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", m.getId());
                    node.put("parentId", m.getParentId());
                    node.put("menuName", m.getMenuName());
                    node.put("menuType", m.getMenuType());
                    node.put("path", m.getPath());
                    node.put("component", m.getComponent());
                    node.put("icon", m.getIcon());
                    node.put("sortOrder", m.getSortOrder());
                    node.put("permission", m.getPermission());
                    node.put("visible", m.getVisible());
                    node.put("status", m.getStatus());
                    node.put("children", buildTree(menus, m.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }
}
