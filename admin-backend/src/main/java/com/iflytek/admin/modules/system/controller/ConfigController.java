package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.entity.SysConfig;
import com.iflytek.admin.modules.system.mapper.SysConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final SysConfigMapper configMapper;

    @Operation(summary = "配置列表")
    @PreAuthorize("hasAuthority('sys:config:list')")
    @GetMapping
    public Result<List<SysConfig>> list() {
        return Result.ok(configMapper.selectList(null));
    }

    @Operation(summary = "批量更新配置")
    @PreAuthorize("hasAuthority('sys:config:edit')")
    @Log(module = "系统配置", operation = "更新")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody List<SysConfig> configs) {
        configs.forEach(configMapper::updateById);
        return Result.ok();
    }
}
