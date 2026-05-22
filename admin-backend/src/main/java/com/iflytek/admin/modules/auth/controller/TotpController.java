package com.iflytek.admin.modules.auth.controller;

import com.iflytek.admin.common.annotation.RateLimiter;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.common.utils.SecurityUtil;
import com.iflytek.admin.modules.auth.dto.TotpSetupResponse;
import com.iflytek.admin.modules.auth.dto.TotpVerifyRequest;
import com.iflytek.admin.modules.auth.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "双因素认证")
@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
public class TotpController {

    private final TotpService totpService;

    @Operation(summary = "生成2FA绑定信息")
    @RateLimiter(count = 5, time = 60)
    @PostMapping("/setup")
    public Result<TotpSetupResponse> setup() {
        Long userId = SecurityUtil.getCurrentUserId();
        String username = SecurityUtil.getCurrentUsername();
        return Result.ok(totpService.generateSetup(userId, username));
    }

    @Operation(summary = "验证并启用2FA")
    @RateLimiter(count = 10, time = 60)
    @PostMapping("/verify")
    public Result<Void> verify(@Valid @RequestBody TotpVerifyRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        totpService.verifyAndEnable(userId, request.getCode());
        return Result.ok();
    }

    @Operation(summary = "禁用2FA")
    @RateLimiter(count = 5, time = 60)
    @DeleteMapping
    public Result<Void> disable() {
        Long userId = SecurityUtil.getCurrentUserId();
        totpService.disable(userId);
        return Result.ok();
    }

    @Operation(summary = "查询2FA状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Long userId = SecurityUtil.getCurrentUserId();
        boolean enabled = totpService.isEnabled(userId);
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", enabled);
        return Result.ok(status);
    }
}