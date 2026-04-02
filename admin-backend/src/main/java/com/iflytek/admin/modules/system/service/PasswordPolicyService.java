package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.modules.system.entity.SysUser;

import java.util.List;

public interface PasswordPolicyService {
    List<String> validate(String rawPassword);
    boolean isHistoryPassword(Long userId, String rawPassword);
    void recordHistory(Long userId, String encodedPassword);
    boolean isExpired(SysUser user);
}
