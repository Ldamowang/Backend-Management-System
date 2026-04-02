package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.dto.JobFormDTO;
import com.iflytek.admin.modules.system.dto.StatusDTO;
import com.iflytek.admin.modules.system.entity.SysJob;
import com.iflytek.admin.modules.system.entity.SysJobLog;
import com.iflytek.admin.modules.system.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "定时任务")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "任务列表")
    @PreAuthorize("hasAuthority('sys:job:list')")
    @GetMapping
    public Result<PageResult<SysJob>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(jobService.page(page, size));
    }

    @Operation(summary = "任务详情")
    @PreAuthorize("hasAuthority('sys:job:list')")
    @GetMapping("/{id}")
    public Result<SysJob> getById(@PathVariable Long id) {
        return Result.ok(jobService.getById(id));
    }

    @Operation(summary = "新增任务")
    @PreAuthorize("hasAuthority('sys:job:add')")
    @Log(module = "定时任务", operation = "新增")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody JobFormDTO dto) {
        jobService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑任务")
    @PreAuthorize("hasAuthority('sys:job:edit')")
    @Log(module = "定时任务", operation = "编辑")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody JobFormDTO dto) {
        jobService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除任务")
    @PreAuthorize("hasAuthority('sys:job:delete')")
    @Log(module = "定时任务", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "切换任务状态")
    @PreAuthorize("hasAuthority('sys:job:edit')")
    @Log(module = "定时任务", operation = "状态变更")
    @PatchMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        jobService.changeStatus(id, dto.getStatus());
        return Result.ok();
    }

    @Operation(summary = "立即执行")
    @PreAuthorize("hasAuthority('sys:job:run')")
    @Log(module = "定时任务", operation = "手动执行")
    @PostMapping("/{id}/run")
    public Result<Void> run(@PathVariable Long id) {
        jobService.run(id);
        return Result.ok();
    }

    @Operation(summary = "任务日志")
    @PreAuthorize("hasAuthority('sys:job:list')")
    @GetMapping("/{id}/logs")
    public Result<PageResult<SysJobLog>> logPage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(jobService.logPage(id, page, size));
    }
}
