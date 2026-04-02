package com.iflytek.admin.common.controller;

import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Tag(name = "幂等性")
@RestController
@RequestMapping("/api/idempotent")
@RequiredArgsConstructor
public class IdempotentController {

    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "获取幂等 Token")
    @GetMapping("/token")
    public Result<String> getToken() {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                CacheConstants.IDEMPOTENT_PREFIX + token, "1", 600, TimeUnit.SECONDS);
        return Result.ok(token);
    }
}
