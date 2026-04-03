package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.modules.system.dto.NoticeFormDTO;
import com.iflytek.admin.modules.system.entity.SysNotice;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.entity.SysUserNotice;
import com.iflytek.admin.modules.system.mapper.SysNoticeMapper;
import com.iflytek.admin.modules.system.mapper.SysUserMapper;
import com.iflytek.admin.modules.system.mapper.SysUserNoticeMapper;
import com.iflytek.admin.modules.system.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final SysNoticeMapper noticeMapper;
    private final SysUserNoticeMapper userNoticeMapper;
    private final SysUserMapper userMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public PageResult<SysNotice> page(int page, int size) {
        Page<SysNotice> result = noticeMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysNotice>().orderByDesc(SysNotice::getCreatedTime));
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public SysNotice getById(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) throw new BusinessException(404, "通知不存在");
        return notice;
    }

    @Override
    public void create(NoticeFormDTO dto) {
        SysNotice notice = new SysNotice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setNoticeType(dto.getNoticeType());
        notice.setStatus(dto.getStatus());
        noticeMapper.insert(notice);
    }

    @Override
    public void update(Long id, NoticeFormDTO dto) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) throw new BusinessException(404, "通知不存在");
        if (dto.getTitle() != null) notice.setTitle(dto.getTitle());
        if (dto.getContent() != null) notice.setContent(dto.getContent());
        if (dto.getNoticeType() != null) notice.setNoticeType(dto.getNoticeType());
        if (dto.getStatus() != null) notice.setStatus(dto.getStatus());
        noticeMapper.updateById(notice);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        noticeMapper.deleteById(id);
        userNoticeMapper.delete(new LambdaQueryWrapper<SysUserNotice>().eq(SysUserNotice::getNoticeId, id));
    }

    @Override
    @Transactional
    public void publish(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) throw new BusinessException(404, "通知不存在");
        notice.setStatus(1);
        noticeMapper.updateById(notice);

        // 给所有活跃用户发送通知
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        for (SysUser user : users) {
            Long count = userNoticeMapper.selectCount(new LambdaQueryWrapper<SysUserNotice>()
                    .eq(SysUserNotice::getUserId, user.getId())
                    .eq(SysUserNotice::getNoticeId, id));
            if (count == 0) {
                SysUserNotice un = new SysUserNotice();
                un.setUserId(user.getId());
                un.setNoticeId(id);
                un.setIsRead(0);
                userNoticeMapper.insert(un);
            }
        }

        // WebSocket 推送
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notice.getId());
        payload.put("title", notice.getTitle());
        payload.put("noticeType", notice.getNoticeType());
        payload.put("createdTime", notice.getCreatedTime());

        if (notice.getNoticeType() == 2) {
            // 公告：广播给所有人
            messagingTemplate.convertAndSend("/topic/notice/broadcast", payload);
        } else {
            // 通知：逐用户推送
            for (SysUser user : users) {
                messagingTemplate.convertAndSendToUser(
                        user.getId().toString(),
                        "/queue/notice",
                        payload);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getMyNotices(Long userId) {
        List<SysUserNotice> userNotices = userNoticeMapper.selectList(
                new LambdaQueryWrapper<SysUserNotice>()
                        .eq(SysUserNotice::getUserId, userId)
                        .orderByAsc(SysUserNotice::getIsRead)
                        .orderByDesc(SysUserNotice::getId));

        if (userNotices.isEmpty()) return List.of();

        List<Long> noticeIds = userNotices.stream().map(SysUserNotice::getNoticeId).collect(Collectors.toList());
        List<SysNotice> notices = noticeMapper.selectBatchIds(noticeIds);
        Map<Long, SysNotice> noticeMap = notices.stream().collect(Collectors.toMap(SysNotice::getId, n -> n));

        return userNotices.stream().map(un -> {
            SysNotice notice = noticeMap.get(un.getNoticeId());
            if (notice == null) return null;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", un.getId());
            item.put("noticeId", notice.getId());
            item.put("title", notice.getTitle());
            item.put("content", notice.getContent());
            item.put("noticeType", notice.getNoticeType());
            item.put("isRead", un.getIsRead());
            item.put("createdTime", notice.getCreatedTime());
            return item;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return userNoticeMapper.selectCount(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsRead, 0));
    }

    @Override
    public void markRead(Long userId, Long noticeId) {
        SysUserNotice un = userNoticeMapper.selectOne(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getNoticeId, noticeId));
        if (un != null && un.getIsRead() == 0) {
            un.setIsRead(1);
            un.setReadTime(LocalDateTime.now());
            userNoticeMapper.updateById(un);
        }
    }

    @Override
    public void markAllRead(Long userId) {
        List<SysUserNotice> unreadList = userNoticeMapper.selectList(new LambdaQueryWrapper<SysUserNotice>()
                .eq(SysUserNotice::getUserId, userId)
                .eq(SysUserNotice::getIsRead, 0));
        LocalDateTime now = LocalDateTime.now();
        for (SysUserNotice un : unreadList) {
            un.setIsRead(1);
            un.setReadTime(now);
            userNoticeMapper.updateById(un);
        }
    }
}
