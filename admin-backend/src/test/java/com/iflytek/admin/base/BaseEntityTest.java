package com.iflytek.admin.base;

import com.iflytek.admin.modules.system.entity.SysUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseEntity 基类字段测试")
class BaseEntityTest {

    @Test
    @DisplayName("所有字段 - Getter/Setter正确工作")
    void allFields() {
        // SysUser extends BaseEntity, testing through concrete subclass
        SysUser user = new SysUser();
        LocalDateTime now = LocalDateTime.now();

        user.setId(1L);
        user.setCreatedBy("admin");
        user.setCreatedTime(now);
        user.setUpdatedBy("admin");
        user.setUpdatedTime(now);
        user.setDeleted(0);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getCreatedBy()).isEqualTo("admin");
        assertThat(user.getCreatedTime()).isEqualTo(now);
        assertThat(user.getUpdatedBy()).isEqualTo("admin");
        assertThat(user.getUpdatedTime()).isEqualTo(now);
        assertThat(user.getDeleted()).isZero();
    }

    @Test
    @DisplayName("默认值 - 新对象字段为null")
    void defaultValues() {
        SysUser user = new SysUser();

        assertThat(user.getId()).isNull();
        assertThat(user.getCreatedBy()).isNull();
        assertThat(user.getCreatedTime()).isNull();
        assertThat(user.getDeleted()).isNull();
    }
}
