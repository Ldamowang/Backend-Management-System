package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.modules.system.dto.MenuFormDTO;
import com.iflytek.admin.modules.system.entity.SysMenu;

import java.util.List;
import java.util.Map;

public interface MenuService {
    List<Map<String, Object>> getMenuTree();
    SysMenu getById(Long id);
    void create(MenuFormDTO dto);
    void update(Long id, MenuFormDTO dto);
    void delete(Long id);
}
