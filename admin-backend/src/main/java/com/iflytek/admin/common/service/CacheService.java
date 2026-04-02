package com.iflytek.admin.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取缓存值并转换为指定类型
     *
     * @param key  缓存键
     * @param type 目标类型
     * @return 缓存值，不存在或异常时返回 null
     */
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return type.cast(value);
        } catch (Exception e) {
            log.warn("获取缓存失败, key={}", key, e);
            return null;
        }
    }

    /**
     * 获取缓存中的 List 对象（unchecked cast）
     *
     * @param key 缓存键
     * @return 缓存的 List，不存在或异常时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getAsList(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return (T) value;
        } catch (Exception e) {
            log.warn("获取缓存List失败, key={}", key, e);
            return null;
        }
    }

    /**
     * 设置缓存值（带 TTL）
     *
     * @param key        缓存键
     * @param value      缓存值
     * @param ttlSeconds 过期时间（秒）
     */
    public void set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("设置缓存失败, key={}", key, e);
        }
    }

    /**
     * 删除单个缓存键
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除缓存失败, key={}", key, e);
        }
    }

    /**
     * 按前缀批量删除缓存（使用 SCAN 代替 KEYS，避免阻塞 Redis）
     *
     * @param prefix 缓存键前缀
     */
    public void deleteByPrefix(String prefix) {
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(prefix + "*")
                    .count(200)
                    .build();
            List<String> keysToDelete = new ArrayList<>();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keysToDelete.add(cursor.next());
                }
            }
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        } catch (Exception e) {
            log.warn("按前缀删除缓存失败, prefix={}", prefix, e);
        }
    }
}
