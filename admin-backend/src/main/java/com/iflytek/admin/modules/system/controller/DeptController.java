package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.dto.DeptFormDTO;
import com.iflytek.admin.modules.system.entity.SysDepartment;
import com.iflytek.admin.modules.system.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "部门树")
    @PreAuthorize("hasAuthority('sys:dept:list')")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(deptService.getDeptTree());
    }

    @Operation(summary = "部门精简树(下拉选择用)")
    @GetMapping("/simple-tree")
    public Result<List<Map<String, Object>>> simpleTree() {
        return Result.ok(deptService.getDeptSimpleTree());
    }

    @Operation(summary = "部门详情")
    @PreAuthorize("hasAuthority('sys:dept:list')")
    @GetMapping("/{id}")
    public Result<SysDepartment> getById(@PathVariable Long id) {
        return Result.ok(deptService.getById(id));
    }

    @Operation(summary = "新增部门")
    @PreAuthorize("hasAuthority('sys:dept:add')")
    @Log(module = "部门管理", operation = "新增")
    @Idempotent
    @PostMapping
    public Result<Void> create(@Valid @RequestBody DeptFormDTO dto) {
        deptService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑部门")
    @PreAuthorize("hasAuthority('sys:dept:edit')")
    @Log(module = "部门管理", operation = "编辑")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeptFormDTO dto) {
        deptService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除部门")
    @PreAuthorize("hasAuthority('sys:dept:delete')")
    @Log(module = "部门管理", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
