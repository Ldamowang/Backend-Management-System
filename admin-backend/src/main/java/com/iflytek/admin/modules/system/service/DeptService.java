package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.modules.system.dto.DeptFormDTO;
import com.iflytek.admin.modules.system.entity.SysDepartment;

import java.util.List;
import java.util.Map;

public interface DeptService {
    List<Map<String, Object>> getDeptTree();
    List<Map<String, Object>> getDeptSimpleTree();
    SysDepartment getById(Long id);
    void create(DeptFormDTO dto);
    void update(Long id, DeptFormDTO dto);
    void delete(Long id);
}
