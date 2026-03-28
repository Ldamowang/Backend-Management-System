package com.iflytek.admin.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.WebDataBinder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebConfig 全局WebDataBinder配置测试")
class WebConfigTest {

    private final WebConfig webConfig = new WebConfig();

    @Test
    @DisplayName("initBinder - 注册StringTrimmerEditor，空字符串转null")
    void initBinder_registersStringTrimmerEditor() {
        WebDataBinder binder = new WebDataBinder(null);

        webConfig.initBinder(binder);

        // StringTrimmerEditor converts empty strings to null
        // Verify the custom editor is registered for String type
        assertThat(binder.findCustomEditor(String.class, null)).isNotNull();
    }
}
