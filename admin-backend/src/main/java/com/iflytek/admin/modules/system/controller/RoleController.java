package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.dto.AssignMenusDTO;
import com.iflytek.admin.modules.system.dto.RoleFormDTO;
import com.iflytek.admin.modules.system.entity.SysRole;
import com.iflytek.admin.modules.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色列表")
    @PreAuthorize("hasAuthority('sys:role:list')")
    @GetMapping
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.list());
    }

    @Operation(summary = "角色详情")
    @PreAuthorize("hasAuthority('sys:role:list')")
    @GetMapping("/{id}")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.ok(roleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PreAuthorize("hasAuthority('sys:role:add')")
    @Log(module = "角色管理", operation = "新增")
    @Idempotent
    @PostMapping
    public Result<Void> create(@Valid @RequestBody RoleFormDTO dto) {
        roleService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑角色")
    @PreAuthorize("hasAuthority('sys:role:edit')")
    @Log(module = "角色管理", operation = "编辑")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleFormDTO dto) {
        roleService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除角色")
    @PreAuthorize("hasAuthority('sys:role:delete')")
    @Log(module = "角色管理", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "分配菜单权限")
    @PreAuthorize("hasAuthority('sys:role:edit')")
    @Log(module = "角色管理", operation = "分配权限")
    @PostMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody AssignMenusDTO dto) {
        roleService.assignMenus(id, dto.getMenuIds());
        return Result.ok();
    }
}
