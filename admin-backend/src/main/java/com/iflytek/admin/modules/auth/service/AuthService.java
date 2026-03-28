package com.iflytek.admin.modules.auth.service;

import com.iflytek.admin.modules.auth.dto.LoginRequest;
import com.iflytek.admin.modules.auth.dto.LoginResponse;

import java.util.Map;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
    LoginResponse refreshToken(String refreshToken);
    Map<String, Object> getUserInfo(Long userId);
}
