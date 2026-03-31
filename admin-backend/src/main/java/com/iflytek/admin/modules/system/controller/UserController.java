package com.iflytek.admin.modules.system.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.dto.StatusDTO;
import com.iflytek.admin.modules.system.dto.UserCreateDTO;
import com.iflytek.admin.modules.system.dto.UserQueryDTO;
import com.iflytek.admin.modules.system.dto.UserUpdateDTO;
import com.iflytek.admin.modules.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页列表")
    @PreAuthorize("hasAuthority('sys:user:list')")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> page(UserQueryDTO query) {
        return Result.ok(userService.page(query));
    }

    @Operation(summary = "用户详情")
    @PreAuthorize("hasAuthority('sys:user:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PreAuthorize("hasAuthority('sys:user:add')")
    @Log(module = "用户管理", operation = "新增")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        userService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑用户")
    @PreAuthorize("hasAuthority('sys:user:edit')")
    @Log(module = "用户管理", operation = "编辑")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @PreAuthorize("hasAuthority('sys:user:delete')")
    @Log(module = "用户管理", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "切换用户状态")
    @PreAuthorize("hasAuthority('sys:user:edit')")
    @Log(module = "用户管理", operation = "状态变更")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        userService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @Operation(summary = "导出用户列表")
    @PreAuthorize("hasAuthority('sys:user:export')")
    @Log(module = "用户管理", operation = "导出")
    @GetMapping("/export")
    public void export(UserQueryDTO query, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> users = userService.listForExport(query);

        // 定义列映射（中文表头 -> 字段名）
        Map<String, String> headerAlias = new LinkedHashMap<>();
        headerAlias.put("id", "ID");
        headerAlias.put("username", "用户名");
        headerAlias.put("nickname", "昵称");
        headerAlias.put("email", "邮箱");
        headerAlias.put("phone", "手机号");
        headerAlias.put("gender", "性别");
        headerAlias.put("status", "状态");
        headerAlias.put("createdTime", "创建时间");

        ExcelWriter writer = ExcelUtil.getWriter(true);
        headerAlias.forEach((field, alias) -> writer.addHeaderAlias(field, alias));
        writer.setOnlyAlias(true);
        writer.write(users, true);
        writer.autoSizeColumnAll();

        String fileName = URLEncoder.encode("用户列表.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        writer.flush(response.getOutputStream(), true);
        writer.close();
    }
}
