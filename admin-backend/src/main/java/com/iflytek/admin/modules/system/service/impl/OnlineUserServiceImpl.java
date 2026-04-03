package com.iflytek.admin.modules.system.service.impl;

import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.system.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listOnlineUsers() {
        Set<String> keys = redisTemplate.keys(CacheConstants.ONLINE_USER_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        for (String key : keys) {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Map) {
                Map<String, Object> userInfo = new LinkedHashMap<>((Map<String, Object>) value);
                String token = key.substring(CacheConstants.ONLINE_USER_PREFIX.length());
                userInfo.put("tokenId", token.substring(0, Math.min(token.length(), 20)) + "...");
                userInfo.put("token", token);
                result.add(userInfo);
            }
        }
        result.sort((a, b) -> String.valueOf(b.get("loginTime")).compareTo(String.valueOf(a.get("loginTime"))));
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void kickOut(String token) {
        Object userInfo = redisTemplate.opsForValue().get(CacheConstants.ONLINE_USER_PREFIX + token);
        if (userInfo instanceof Map) {
            Long userId = ((Number) ((Map<?, ?>) userInfo).get("userId")).longValue();
            removeSession(userId, token);
        } else {
            redisTemplate.delete(CacheConstants.ONLINE_USER_PREFIX + token);
        }
        // 将 token 加入黑名单
        redisTemplate.opsForValue().set(
                CacheConstants.TOKEN_BLACKLIST_PREFIX + token,
                "kicked",
                jwtUtil.getAccessTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void addSession(Long userId, String token, Map<String, Object> userInfo) {
        String sessionsKey = CacheConstants.USER_SESSIONS_PREFIX + userId;
        String tokenKey = CacheConstants.ONLINE_USER_PREFIX + token;

        // Add token to user's session sorted set (score = current timestamp)
        redisTemplate.opsForZSet().add(sessionsKey, token, System.currentTimeMillis());
        redisTemplate.expire(sessionsKey, Duration.ofDays(7));

        // Store token detail (keep existing pattern)
        redisTemplate.opsForValue().set(tokenKey, userInfo,
                Duration.ofMillis(jwtUtil.getAccessTokenExpiration()));
    }

    @Override
    public void removeSession(Long userId, String token) {
        String sessionsKey = CacheConstants.USER_SESSIONS_PREFIX + userId;
        redisTemplate.opsForZSet().remove(sessionsKey, token);
        redisTemplate.delete(CacheConstants.ONLINE_USER_PREFIX + token);
    }

    @Override
    public long getSessionCount(Long userId) {
        String sessionsKey = CacheConstants.USER_SESSIONS_PREFIX + userId;
        Long count = redisTemplate.opsForZSet().zCard(sessionsKey);
        return count != null ? count : 0;
    }

    @Override
    public String kickOldestSession(Long userId) {
        String sessionsKey = CacheConstants.USER_SESSIONS_PREFIX + userId;
        var oldest = redisTemplate.opsForZSet().rangeWithScores(sessionsKey, 0, 0);
        if (oldest != null && !oldest.isEmpty()) {
            var entry = oldest.iterator().next();
            String oldToken = (String) entry.getValue();
            removeSession(userId, oldToken);
            // Blacklist the kicked token
            redisTemplate.opsForValue().set(
                    CacheConstants.TOKEN_BLACKLIST_PREFIX + oldToken, "kicked",
                    Duration.ofMillis(jwtUtil.getAccessTokenExpiration()));
            return oldToken;
        }
        return null;
    }
}
