package com.iflytek.admin.modules.system.service;

import java.util.List;
import java.util.Map;

public interface OnlineUserService {
    List<Map<String, Object>> listOnlineUsers();
    void kickOut(String token);

    /** Record a user session in Redis Sorted Set + token detail */
    void addSession(Long userId, String token, Map<String, Object> userInfo);

    /** Remove a session */
    void removeSession(Long userId, String token);

    /** Get current session count for a user */
    long getSessionCount(Long userId);

    /** Kick the oldest session (lowest score = earliest login). Returns the kicked token, or null if no sessions. */
    String kickOldestSession(Long userId);
}
