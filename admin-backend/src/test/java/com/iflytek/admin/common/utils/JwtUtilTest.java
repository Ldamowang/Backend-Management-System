package com.iflytek.admin.common.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("JwtUtil 工具类测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "ThisIsAVeryLongSecretKeyForTestingPurposesOnly12345");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 1800000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 604800000L);
    }

    @Nested
    @DisplayName("Token 生成测试")
    class GenerateTokenTests {

        @Test
        @DisplayName("生成 AccessToken - 包含正确的 claims")
        void generateAccessToken() {
            String token = jwtUtil.generateAccessToken(1L, "admin");

            assertThat(token).isNotBlank();
            Claims claims = jwtUtil.parseToken(token);
            assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
            assertThat(claims.get("username", String.class)).isEqualTo("admin");
            assertThat(claims.get("type", String.class)).isEqualTo("access");
        }

        @Test
        @DisplayName("生成 RefreshToken - 包含正确的 claims")
        void generateRefreshToken() {
            String token = jwtUtil.generateRefreshToken(1L, "admin");

            assertThat(token).isNotBlank();
            Claims claims = jwtUtil.parseToken(token);
            assertThat(claims.get("type", String.class)).isEqualTo("refresh");
            assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
            assertThat(claims.get("username", String.class)).isEqualTo("admin");
        }
    }

    @Nested
    @DisplayName("Token 解析测试")
    class ParseTokenTests {

        @Test
        @DisplayName("解析有效Token - 返回正确的 userId")
        void getUserId() {
            String token = jwtUtil.generateAccessToken(42L, "testuser");
            assertThat(jwtUtil.getUserId(token)).isEqualTo(42L);
        }

        @Test
        @DisplayName("解析有效Token - 返回正确的 username")
        void getUsername() {
            String token = jwtUtil.generateAccessToken(1L, "admin");
            assertThat(jwtUtil.getUsername(token)).isEqualTo("admin");
        }

        @Test
        @DisplayName("解析无效Token - 抛出异常")
        void parseInvalidToken() {
            assertThatThrownBy(() -> jwtUtil.parseToken("invalid-token"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Token 验证测试")
    class ValidateTokenTests {

        @Test
        @DisplayName("有效Token - 返回true")
        void isTokenValid_valid() {
            String token = jwtUtil.generateAccessToken(1L, "admin");
            assertThat(jwtUtil.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("无效Token - 返回false")
        void isTokenValid_invalid() {
            assertThat(jwtUtil.isTokenValid("invalid")).isFalse();
        }

        @Test
        @DisplayName("过期Token - 返回false")
        void isTokenValid_expired() {
            ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", -1000L);
            String token = jwtUtil.generateAccessToken(1L, "admin");
            assertThat(jwtUtil.isTokenValid(token)).isFalse();
        }
    }

    @Test
    @DisplayName("getAccessTokenExpiration - 返回正确值")
    void getAccessTokenExpiration() {
        assertThat(jwtUtil.getAccessTokenExpiration()).isEqualTo(1800000L);
    }

    @Nested
    @DisplayName("Token类型测试")
    class TokenTypeTests {

        @Test
        @DisplayName("AccessToken类型 - 返回access")
        void getTokenType_access() {
            String token = jwtUtil.generateAccessToken(1L, "admin");
            assertThat(jwtUtil.getTokenType(token)).isEqualTo("access");
        }

        @Test
        @DisplayName("RefreshToken类型 - 返回refresh")
        void getTokenType_refresh() {
            String token = jwtUtil.generateRefreshToken(1L, "admin");
            assertThat(jwtUtil.getTokenType(token)).isEqualTo("refresh");
        }
    }

    @Nested
    @DisplayName("Secret验证测试")
    class ValidateSecretTests {

        @Test
        @DisplayName("有效Secret - 不抛异常")
        void validateSecret_valid() {
            JwtUtil util = new JwtUtil();
            ReflectionTestUtils.setField(util, "secret",
                    "ThisIsAVeryLongSecretKeyForTestingPurposesOnly12345");

            assertThatCode(util::validateSecret).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("空Secret - 抛出异常")
        void validateSecret_blank() {
            JwtUtil util = new JwtUtil();
            ReflectionTestUtils.setField(util, "secret", "");

            assertThatThrownBy(util::validateSecret)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT secret 未配置");
        }

        @Test
        @DisplayName("null Secret - 抛出异常")
        void validateSecret_null() {
            JwtUtil util = new JwtUtil();
            ReflectionTestUtils.setField(util, "secret", null);

            assertThatThrownBy(util::validateSecret)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT secret 未配置");
        }

        @Test
        @DisplayName("Secret太短 - 抛出异常")
        void validateSecret_tooShort() {
            JwtUtil util = new JwtUtil();
            ReflectionTestUtils.setField(util, "secret", "short");

            assertThatThrownBy(util::validateSecret)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("长度不足");
        }
    }
}
