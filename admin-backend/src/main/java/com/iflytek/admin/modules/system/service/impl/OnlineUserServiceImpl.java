package com.iflytek.admin.modules.system.service.impl;

import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.system.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
    public void kickOut(String token) {
        // 移除在线用户记录
        redisTemplate.delete(CacheConstants.ONLINE_USER_PREFIX + token);
        // 将 token 加入黑名单
        redisTemplate.opsForValue().set(
                CacheConstants.TOKEN_BLACKLIST_PREFIX + token,
                "1",
                jwtUtil.getAccessTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
    }
}
