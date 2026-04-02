package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.service.CacheService;
import com.iflytek.admin.modules.system.dto.RoleFormDTO;
import com.iflytek.admin.modules.system.entity.SysRole;
import com.iflytek.admin.modules.system.entity.SysRoleMenu;
import com.iflytek.admin.modules.system.mapper.SysRoleMapper;
import com.iflytek.admin.modules.system.mapper.SysRoleMenuMapper;
import com.iflytek.admin.modules.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final CacheService cacheService;

    @Override
    public List<SysRole> list() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSortOrder));
    }

    @Override
    public SysRole getById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(404, "角色不存在");
        return role;
    }

    @Override
    public void create(RoleFormDTO dto) {
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setSortOrder(dto.getSortOrder());
        role.setStatus(dto.getStatus());
        role.setDescription(dto.getDescription());
        roleMapper.insert(role);
    }

    @Override
    public void update(Long id, RoleFormDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException(404, "角色不存在");
        if (dto.getRoleName() != null) role.setRoleName(dto.getRoleName());
        if (dto.getRoleKey() != null) role.setRoleKey(dto.getRoleKey());
        if (dto.getSortOrder() != null) role.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) role.setStatus(dto.getStatus());
        if (dto.getDescription() != null) role.setDescription(dto.getDescription());
        roleMapper.updateById(role);
        cacheService.deleteByPrefix(CacheConstants.MENU_USER_PREFIX);
        cacheService.deleteByPrefix(CacheConstants.PERM_USER_PREFIX);
    }

    @Override
    public void delete(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        cacheService.deleteByPrefix(CacheConstants.MENU_USER_PREFIX);
        cacheService.deleteByPrefix(CacheConstants.PERM_USER_PREFIX);
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null) {
            menuIds.forEach(menuId -> {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            });
        }
        cacheService.deleteByPrefix(CacheConstants.MENU_USER_PREFIX);
        cacheService.deleteByPrefix(CacheConstants.PERM_USER_PREFIX);
    }
}
