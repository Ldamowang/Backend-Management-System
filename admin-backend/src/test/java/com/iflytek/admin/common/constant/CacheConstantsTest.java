package com.iflytek.admin.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheConstants 缓存常量测试")
class CacheConstantsTest {

    @Test
    @DisplayName("TOKEN_BLACKLIST_PREFIX 值正确")
    void tokenBlacklistPrefix() {
        assertThat(CacheConstants.TOKEN_BLACKLIST_PREFIX).isEqualTo("token:blacklist:");
    }

    @Test
    @DisplayName("LOGIN_USER_PREFIX 值正确")
    void loginUserPrefix() {
        assertThat(CacheConstants.LOGIN_USER_PREFIX).isEqualTo("login:user:");
    }
}
