package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.dto.MenuFormDTO;
import com.iflytek.admin.modules.system.entity.SysMenu;
import com.iflytek.admin.modules.system.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "菜单树")
    @PreAuthorize("hasAuthority('sys:menu:list')")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> getMenuTree() {
        return Result.ok(menuService.getMenuTree());
    }

    @Operation(summary = "菜单详情")
    @PreAuthorize("hasAuthority('sys:menu:list')")
    @GetMapping("/{id}")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.ok(menuService.getById(id));
    }

    @Operation(summary = "新增菜单")
    @PreAuthorize("hasAuthority('sys:menu:add')")
    @Log(module = "菜单管理", operation = "新增")
    @Idempotent
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MenuFormDTO dto) {
        menuService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑菜单")
    @PreAuthorize("hasAuthority('sys:menu:edit')")
    @Log(module = "菜单管理", operation = "编辑")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MenuFormDTO dto) {
        menuService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除菜单")
    @PreAuthorize("hasAuthority('sys:menu:delete')")
    @Log(module = "菜单管理", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
