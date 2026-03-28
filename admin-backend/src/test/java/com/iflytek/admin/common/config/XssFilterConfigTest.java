package com.iflytek.admin.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XssFilterConfig XSS过滤器配置测试")
class XssFilterConfigTest {

    private final XssFilterConfig config = new XssFilterConfig();

    @Test
    @DisplayName("创建XssFilter注册Bean - 配置正确")
    void xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = config.xssFilterRegistration();

        assertThat(registration).isNotNull();
        assertThat(registration.getFilter()).isInstanceOf(XssFilter.class);
        assertThat(registration.getUrlPatterns()).contains("/*");
        assertThat(registration.getOrder()).isEqualTo(1);
    }
}
