package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.modules.system.dto.UserCreateDTO;
import com.iflytek.admin.modules.system.dto.UserQueryDTO;
import com.iflytek.admin.modules.system.dto.UserUpdateDTO;

import java.util.List;
import java.util.Map;

public interface UserService {
    PageResult<Map<String, Object>> page(UserQueryDTO query);
    Map<String, Object> getById(Long id);
    void create(UserCreateDTO dto);
    void update(Long id, UserUpdateDTO dto);
    void delete(Long id);
    void updateStatus(Long id, Integer status);
    List<Map<String, Object>> listForExport(UserQueryDTO query);
}
