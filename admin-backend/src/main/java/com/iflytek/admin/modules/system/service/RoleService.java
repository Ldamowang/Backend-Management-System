package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.modules.system.dto.RoleFormDTO;
import com.iflytek.admin.modules.system.entity.SysRole;

import java.util.List;

public interface RoleService {
    List<SysRole> list();
    SysRole getById(Long id);
    void create(RoleFormDTO dto);
    void update(Long id, RoleFormDTO dto);
    void delete(Long id);
    void assignMenus(Long roleId, List<Long> menuIds);
}
