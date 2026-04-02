package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.modules.system.entity.SysPasswordHistory;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.mapper.SysPasswordHistoryMapper;
import com.iflytek.admin.modules.system.service.ConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyServiceImplTest {

    @InjectMocks
    private PasswordPolicyServiceImpl service;

    @Mock
    private ConfigService configService;

    @Mock
    private SysPasswordHistoryMapper historyMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private void mockDefaultConfig() {
        when(configService.getValueByKey("pwd.min.length")).thenReturn("8");
        when(configService.getValueByKey("pwd.require.uppercase")).thenReturn("true");
        when(configService.getValueByKey("pwd.require.lowercase")).thenReturn("true");
        when(configService.getValueByKey("pwd.require.digit")).thenReturn("true");
        when(configService.getValueByKey("pwd.require.special")).thenReturn("true");
    }

    @Nested
    @DisplayName("validate - 密码校验")
    class ValidateTests {

        @Test
        @DisplayName("合法密码 - 返回空列表")
        void validate_validPassword_returnsEmpty() {
            mockDefaultConfig();

            List<String> errors = service.validate("Abcdef1!");

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("密码过短 - 返回长度错误")
        void validate_tooShort_returnsLengthError() {
            mockDefaultConfig();

            List<String> errors = service.validate("Ab1!");

            assertThat(errors).anyMatch(e -> e.contains("至少需要 8 个字符"));
        }

        @Test
        @DisplayName("缺少大写字母 - 返回大写错误")
        void validate_missingUppercase_returnsError() {
            mockDefaultConfig();

            List<String> errors = service.validate("abcdefg1!");

            assertThat(errors).anyMatch(e -> e.contains("大写字母"));
        }

        @Test
        @DisplayName("缺少小写字母 - 返回小写错误")
        void validate_missingLowercase_returnsError() {
            mockDefaultConfig();

            List<String> errors = service.validate("ABCDEFG1!");

            assertThat(errors).anyMatch(e -> e.contains("小写字母"));
        }

        @Test
        @DisplayName("缺少数字 - 返回数字错误")
        void validate_missingDigit_returnsError() {
            mockDefaultConfig();

            List<String> errors = service.validate("Abcdefgh!");

            assertThat(errors).anyMatch(e -> e.contains("数字"));
        }

        @Test
        @DisplayName("缺少特殊字符 - 返回特殊字符错误")
        void validate_missingSpecial_returnsError() {
            mockDefaultConfig();

            List<String> errors = service.validate("Abcdefg1");

            assertThat(errors).anyMatch(e -> e.contains("特殊字符"));
        }

        @Test
        @DisplayName("不要求特殊字符时 - 不校验特殊字符")
        void validate_specialNotRequired_noError() {
            when(configService.getValueByKey("pwd.min.length")).thenReturn("8");
            when(configService.getValueByKey("pwd.require.uppercase")).thenReturn("true");
            when(configService.getValueByKey("pwd.require.lowercase")).thenReturn("true");
            when(configService.getValueByKey("pwd.require.digit")).thenReturn("true");
            when(configService.getValueByKey("pwd.require.special")).thenReturn("false");

            List<String> errors = service.validate("Abcdefg1");

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("多条规则同时不满足 - 返回多个错误")
        void validate_multipleViolations_returnsMultipleErrors() {
            mockDefaultConfig();

            List<String> errors = service.validate("abc");

            assertThat(errors).hasSizeGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("配置为null时使用默认值 - 最小长度默认8")
        void validate_nullConfig_usesDefaults() {
            when(configService.getValueByKey("pwd.min.length")).thenReturn(null);
            when(configService.getValueByKey("pwd.require.uppercase")).thenReturn(null);
            when(configService.getValueByKey("pwd.require.lowercase")).thenReturn(null);
            when(configService.getValueByKey("pwd.require.digit")).thenReturn(null);
            when(configService.getValueByKey("pwd.require.special")).thenReturn(null);

            List<String> errors = service.validate("Abcdefg1!");

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("isHistoryPassword - 历史密码检查")
    class IsHistoryPasswordTests {

        @Test
        @DisplayName("匹配历史密码 - 返回true")
        void isHistoryPassword_matches_returnsTrue() {
            when(configService.getValueByKey("pwd.history.count")).thenReturn("5");

            SysPasswordHistory history1 = new SysPasswordHistory();
            history1.setPassword("$2a$encoded1");
            SysPasswordHistory history2 = new SysPasswordHistory();
            history2.setPassword("$2a$encoded2");

            when(historyMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(history1, history2));
            when(passwordEncoder.matches("newPassword", "$2a$encoded1")).thenReturn(false);
            when(passwordEncoder.matches("newPassword", "$2a$encoded2")).thenReturn(true);

            boolean result = service.isHistoryPassword(1L, "newPassword");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不匹配历史密码 - 返回false")
        void isHistoryPassword_noMatch_returnsFalse() {
            when(configService.getValueByKey("pwd.history.count")).thenReturn("5");

            SysPasswordHistory history1 = new SysPasswordHistory();
            history1.setPassword("$2a$encoded1");

            when(historyMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(history1));
            when(passwordEncoder.matches("newPassword", "$2a$encoded1")).thenReturn(false);

            boolean result = service.isHistoryPassword(1L, "newPassword");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("无历史记录 - 返回false")
        void isHistoryPassword_noHistory_returnsFalse() {
            when(configService.getValueByKey("pwd.history.count")).thenReturn("5");
            when(historyMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            boolean result = service.isHistoryPassword(1L, "newPassword");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("历史记录数配置为null - 使用默认值")
        void isHistoryPassword_nullConfig_usesDefault() {
            when(configService.getValueByKey("pwd.history.count")).thenReturn(null);
            when(historyMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            boolean result = service.isHistoryPassword(1L, "newPassword");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired - 密码过期检查")
    class IsExpiredTests {

        @Test
        @DisplayName("过期天数为0 - 永不过期")
        void isExpired_zeroDays_neverExpires() {
            when(configService.getValueByKey("pwd.expire.days")).thenReturn("0");

            SysUser user = new SysUser();
            user.setPasswordChangedTime(LocalDateTime.now().minusDays(365));

            boolean result = service.isExpired(user);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("passwordChangedTime为null - 已过期")
        void isExpired_nullChangedTime_expired() {
            when(configService.getValueByKey("pwd.expire.days")).thenReturn("90");

            SysUser user = new SysUser();
            user.setPasswordChangedTime(null);

            boolean result = service.isExpired(user);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("在有效期内 - 未过期")
        void isExpired_withinPeriod_notExpired() {
            when(configService.getValueByKey("pwd.expire.days")).thenReturn("90");

            SysUser user = new SysUser();
            user.setPasswordChangedTime(LocalDateTime.now().minusDays(30));

            boolean result = service.isExpired(user);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("超过有效期 - 已过期")
        void isExpired_pastPeriod_expired() {
            when(configService.getValueByKey("pwd.expire.days")).thenReturn("90");

            SysUser user = new SysUser();
            user.setPasswordChangedTime(LocalDateTime.now().minusDays(91));

            boolean result = service.isExpired(user);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("过期天数配置为null - 默认不过期")
        void isExpired_nullConfig_defaultNeverExpires() {
            when(configService.getValueByKey("pwd.expire.days")).thenReturn(null);

            SysUser user = new SysUser();
            user.setPasswordChangedTime(LocalDateTime.now().minusDays(365));

            boolean result = service.isExpired(user);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("recordHistory - 记录密码历史")
    class RecordHistoryTests {

        @Test
        @DisplayName("插入密码历史记录")
        void recordHistory_insertsRecord() {
            service.recordHistory(1L, "$2a$encodedPassword");

            verify(historyMapper).insert(argThat(history -> {
                SysPasswordHistory h = (SysPasswordHistory) history;
                return h.getUserId().equals(1L)
                        && "$2a$encodedPassword".equals(h.getPassword())
                        && h.getCreatedTime() != null;
            }));
        }

        @Test
        @DisplayName("不同用户ID插入各自记录")
        void recordHistory_differentUsers() {
            service.recordHistory(1L, "$2a$encoded1");
            service.recordHistory(2L, "$2a$encoded2");

            verify(historyMapper, times(2)).insert(any(SysPasswordHistory.class));
        }
    }
}
