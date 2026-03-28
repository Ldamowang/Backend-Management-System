package com.iflytek.admin.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.admin.base.PageQuery;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.entity.SysLoginLog;
import com.iflytek.admin.modules.system.entity.SysOperationLog;
import com.iflytek.admin.modules.system.mapper.SysLoginLogMapper;
import com.iflytek.admin.modules.system.mapper.SysOperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "日志管理")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final SysOperationLogMapper operationLogMapper;
    private final SysLoginLogMapper loginLogMapper;

    @Operation(summary = "操作日志分页")
    @PreAuthorize("hasAuthority('sys:log:list')")
    @GetMapping("/operation")
    public Result<PageResult<SysOperationLog>> operationLogs(PageQuery query) {
        Page<SysOperationLog> page = operationLogMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<SysOperationLog>().orderByDesc(SysOperationLog::getCreatedTime));
        return Result.ok(new PageResult<>(page.getRecords(), page.getTotal(), query.getPage(), query.getSize()));
    }

    @Operation(summary = "登录日志分页")
    @PreAuthorize("hasAuthority('sys:log:list')")
    @GetMapping("/login")
    public Result<PageResult<SysLoginLog>> loginLogs(PageQuery query) {
        Page<SysLoginLog> page = loginLogMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<SysLoginLog>().orderByDesc(SysLoginLog::getLoginTime));
        return Result.ok(new PageResult<>(page.getRecords(), page.getTotal(), query.getPage(), query.getSize()));
    }
}
