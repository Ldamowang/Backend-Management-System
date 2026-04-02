package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.modules.system.dto.NoticeFormDTO;
import com.iflytek.admin.modules.system.entity.SysNotice;

import java.util.List;
import java.util.Map;

public interface NoticeService {
    PageResult<SysNotice> page(int page, int size);
    SysNotice getById(Long id);
    void create(NoticeFormDTO dto);
    void update(Long id, NoticeFormDTO dto);
    void delete(Long id);
    void publish(Long id);
    List<Map<String, Object>> getMyNotices(Long userId);
    long getUnreadCount(Long userId);
    void markRead(Long userId, Long noticeId);
    void markAllRead(Long userId);
}
