package com.iflytek.admin.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("MyBatisPlusConfig 配置测试")
class MyBatisPlusConfigTest {

    private final MyBatisPlusConfig config = new MyBatisPlusConfig();

    @Test
    @DisplayName("创建MybatisPlusInterceptor Bean - 包含分页插件")
    void mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

        assertThat(interceptor).isNotNull();
        assertThat(interceptor.getInterceptors()).isNotEmpty();
    }

    @Test
    @DisplayName("创建MetaObjectHandler Bean - 不为null")
    void metaObjectHandler_notNull() {
        MetaObjectHandler handler = config.metaObjectHandler();

        assertThat(handler).isNotNull();
    }
}
