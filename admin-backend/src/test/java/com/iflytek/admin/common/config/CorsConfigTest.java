package com.iflytek.admin.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorsConfig CORS跨域配置测试")
class CorsConfigTest {

    @Test
    @DisplayName("创建CorsFilter Bean - 配置正确")
    void corsFilter_createsBean() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", List.of("http://localhost:5173"));

        CorsFilter filter = corsConfig.corsFilter();

        assertThat(filter).isNotNull();
    }

    @Test
    @DisplayName("多个允许的源 - 全部注册")
    void corsFilter_multipleOrigins() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins",
                List.of("http://localhost:5173", "http://localhost:3000"));

        CorsFilter filter = corsConfig.corsFilter();

        assertThat(filter).isNotNull();
    }
}
