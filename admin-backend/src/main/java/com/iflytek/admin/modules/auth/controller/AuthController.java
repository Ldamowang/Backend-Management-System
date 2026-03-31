package com.iflytek.admin.modules.auth.controller;

import com.iflytek.admin.common.annotation.RateLimiter;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.common.utils.SecurityUtil;
import com.iflytek.admin.modules.auth.dto.LoginRequest;
import com.iflytek.admin.modules.auth.dto.LoginResponse;
import com.iflytek.admin.modules.auth.dto.RefreshTokenDTO;
import com.iflytek.admin.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @RateLimiter(count = 5, time = 60)
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "用户登出")
    @RateLimiter(count = 10, time = 60)
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(request.getHeader("Authorization"));
        return Result.ok();
    }

    @Operation(summary = "刷新Token")
    @RateLimiter(count = 10, time = 60)
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.ok(authService.refreshToken(dto.getRefreshToken()));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userinfo")
    public Result<Map<String, Object>> getUserInfo() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.ok(authService.getUserInfo(userId));
    }
}
