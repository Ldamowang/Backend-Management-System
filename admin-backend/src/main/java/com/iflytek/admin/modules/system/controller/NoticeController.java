package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.annotation.Idempotent;
import com.iflytek.admin.common.annotation.Log;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.common.utils.SecurityUtil;
import com.iflytek.admin.modules.system.dto.NoticeFormDTO;
import com.iflytek.admin.modules.system.entity.SysNotice;
import com.iflytek.admin.modules.system.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "通知公告")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "通知列表(管理)")
    @PreAuthorize("hasAuthority('sys:notice:list')")
    @GetMapping
    public Result<PageResult<SysNotice>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(noticeService.page(page, size));
    }

    @Operation(summary = "通知详情")
    @PreAuthorize("hasAuthority('sys:notice:list')")
    @GetMapping("/{id}")
    public Result<SysNotice> getById(@PathVariable Long id) {
        return Result.ok(noticeService.getById(id));
    }

    @Operation(summary = "新增通知")
    @PreAuthorize("hasAuthority('sys:notice:add')")
    @Log(module = "通知公告", operation = "新增")
    @Idempotent
    @PostMapping
    public Result<Void> create(@Valid @RequestBody NoticeFormDTO dto) {
        noticeService.create(dto);
        return Result.ok();
    }

    @Operation(summary = "编辑通知")
    @PreAuthorize("hasAuthority('sys:notice:edit')")
    @Log(module = "通知公告", operation = "编辑")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeFormDTO dto) {
        noticeService.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除通知")
    @PreAuthorize("hasAuthority('sys:notice:delete')")
    @Log(module = "通知公告", operation = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "发布通知")
    @PreAuthorize("hasAuthority('sys:notice:edit')")
    @Log(module = "通知公告", operation = "发布")
    @PatchMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return Result.ok();
    }

    // ========== 用户端接口 ==========

    @Operation(summary = "我的通知")
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> myNotices() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.ok(noticeService.getMyNotices(userId));
    }

    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.ok(noticeService.getUnreadCount(userId));
    }

    @Operation(summary = "标记已读")
    @PatchMapping("/{noticeId}/read")
    public Result<Void> markRead(@PathVariable Long noticeId) {
        Long userId = SecurityUtil.getCurrentUserId();
        noticeService.markRead(userId, noticeId);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读")
    @PatchMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        noticeService.markAllRead(userId);
        return Result.ok();
    }
}
