package com.iflytek.admin.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CacheService 缓存工具类测试")
class CacheServiceTest {

    @InjectMocks
    private CacheService cacheService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Nested
    @DisplayName("get() 方法测试")
    class GetTests {

        @Test
        @DisplayName("key存在 - 返回对应值")
        void get_keyExists_returnsValue() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("test:key")).thenReturn("hello");

            String result = cacheService.get("test:key", String.class);

            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("key不存在 - 返回null")
        void get_keyMissing_returnsNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("missing:key")).thenReturn(null);

            String result = cacheService.get("missing:key", String.class);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Redis异常 - 返回null而非抛出异常")
        void get_redisError_returnsNull() {
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

            String result = cacheService.get("error:key", String.class);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("set() 方法测试")
    class SetTests {

        @Test
        @DisplayName("正常存储 - 设置值和TTL")
        void set_storesValueWithTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            cacheService.set("test:key", "value", 300);

            verify(valueOperations).set("test:key", "value", 300, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("Redis异常 - 静默失败不抛出异常")
        void set_redisError_silentlyFails() {
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

            // 不应抛出异常
            cacheService.set("error:key", "value", 300);
        }
    }

    @Nested
    @DisplayName("delete() 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除单个key")
        void delete_deletesKey() {
            cacheService.delete("test:key");

            verify(redisTemplate).delete("test:key");
        }

        @Test
        @DisplayName("Redis异常 - 静默失败")
        void delete_redisError_silentlyFails() {
            when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

            // 不应抛出异常
            cacheService.delete("error:key");
        }
    }

    @Nested
    @DisplayName("deleteByPrefix() 方法测试")
    class DeleteByPrefixTests {

        @SuppressWarnings("unchecked")
        private Cursor<String> mockCursor(List<String> keys) {
            Cursor<String> cursor = mock(Cursor.class);
            Iterator<String> iterator = keys.iterator();
            when(cursor.hasNext()).thenAnswer(inv -> iterator.hasNext());
            if (!keys.isEmpty()) {
                when(cursor.next()).thenAnswer(inv -> iterator.next());
            }
            return cursor;
        }

        @Test
        @DisplayName("找到匹配key - 批量删除")
        void deleteByPrefix_findsAndDeletes() {
            List<String> keys = List.of("dict:data:gender", "dict:data:status");
            Cursor<String> cursor = mockCursor(keys);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            cacheService.deleteByPrefix("dict:data:");

            verify(redisTemplate).delete(keys);
        }

        @Test
        @DisplayName("无匹配key - 不调用delete")
        void deleteByPrefix_noKeys_noDeleteCall() {
            Cursor<String> cursor = mockCursor(Collections.emptyList());
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            cacheService.deleteByPrefix("empty:");

            verify(redisTemplate, never()).delete(anyCollection());
        }

        @Test
        @DisplayName("scan返回null - 静默失败")
        void deleteByPrefix_nullCursor_silentlyFails() {
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(null);

            // 不应抛出异常（会被 catch 捕获）
            cacheService.deleteByPrefix("null:");

            verify(redisTemplate, never()).delete(anyCollection());
        }

        @Test
        @DisplayName("Redis异常 - 静默失败")
        void deleteByPrefix_redisError_silentlyFails() {
            when(redisTemplate.scan(any(ScanOptions.class))).thenThrow(new RuntimeException("Redis connection failed"));

            // 不应抛出异常
            cacheService.deleteByPrefix("error:");
        }
    }

    @Nested
    @DisplayName("getAsList() 方法测试")
    class GetAsListTests {

        @Test
        @DisplayName("返回List对象")
        void getAsList_returnsList() {
            List<String> cachedList = List.of("item1", "item2", "item3");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("list:key")).thenReturn(cachedList);

            List<String> result = cacheService.getAsList("list:key");

            assertThat(result).containsExactly("item1", "item2", "item3");
        }

        @Test
        @DisplayName("key不存在 - 返回null")
        void getAsList_keyMissing_returnsNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("missing:key")).thenReturn(null);

            List<String> result = cacheService.getAsList("missing:key");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Redis异常 - 返回null而非抛出异常")
        void getAsList_redisError_returnsNull() {
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

            List<String> result = cacheService.getAsList("error:key");

            assertThat(result).isNull();
        }
    }
}
