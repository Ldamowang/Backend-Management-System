package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.dto.ImportResult;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.common.service.ExcelExportService;
import com.iflytek.admin.common.service.ExcelImportService;
import com.iflytek.admin.modules.system.dto.*;
import com.iflytek.admin.modules.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

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
    @Idempotent
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
        List<UserExportDTO> exportList = users.stream().map(u -> {
            UserExportDTO dto = new UserExportDTO();
            dto.setUsername((String) u.get("username"));
            dto.setNickname((String) u.get("nickname"));
            dto.setEmail((String) u.get("email"));
            dto.setPhone((String) u.get("phone"));
            dto.setGender((Integer) u.get("gender"));
            dto.setStatus((Integer) u.get("status"));
            return dto;
        }).collect(Collectors.toList());
        excelExportService.export(response, "用户列表", exportList, UserExportDTO.class);
    }

    @Operation(summary = "下载用户导入模板")
    @PreAuthorize("hasAuthority('sys:user:add')")
    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        excelExportService.exportTemplate(response, "用户", UserExportDTO.class);
    }

    @Operation(summary = "导入用户")
    @PreAuthorize("hasAuthority('sys:user:add')")
    @Log(module = "用户管理", operation = "导入")
    @PostMapping("/import")
    public Result<ImportResult> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        ImportResult result = excelImportService.importData(
                file,
                UserExportDTO.class,
                dto -> {
                    if (userService.existsByUsername(dto.getUsername())) {
                        return "用户名 " + dto.getUsername() + " 已存在";
                    }
                    return null;
                },
                validList -> userService.batchImport(validList)
        );
        return Result.ok(result);
    }
}
