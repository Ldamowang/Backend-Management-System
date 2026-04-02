package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.service.OnlineUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "在线用户管理")
@RestController
@RequestMapping("/api/online-users")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    @Operation(summary = "在线用户列表")
    @PreAuthorize("hasAuthority('sys:online:list')")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(onlineUserService.listOnlineUsers());
    }

    @Operation(summary = "强制踢出用户")
    @PreAuthorize("hasAuthority('sys:online:kick')")
    @Log(module = "在线用户", operation = "强制踢出")
    @DeleteMapping("/{token}")
    public Result<Void> kickOut(@PathVariable String token) {
        onlineUserService.kickOut(token);
        return Result.ok();
    }
}
