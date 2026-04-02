package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.modules.system.entity.SysPasswordHistory;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.mapper.SysPasswordHistoryMapper;
import com.iflytek.admin.modules.system.service.ConfigService;
import com.iflytek.admin.modules.system.service.PasswordPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    private final ConfigService configService;
    private final SysPasswordHistoryMapper historyMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<String> validate(String rawPassword) {
        List<String> errors = new ArrayList<>();

        int minLength = getIntConfig("pwd.min.length", 8);
        boolean requireUppercase = getBoolConfig("pwd.require.uppercase", true);
        boolean requireLowercase = getBoolConfig("pwd.require.lowercase", true);
        boolean requireDigit = getBoolConfig("pwd.require.digit", true);
        boolean requireSpecial = getBoolConfig("pwd.require.special", true);

        if (rawPassword.length() < minLength) {
            errors.add("密码至少需要 " + minLength + " 个字符");
        }
        if (requireUppercase && !rawPassword.chars().anyMatch(Character::isUpperCase)) {
            errors.add("密码必须包含至少一个大写字母");
        }
        if (requireLowercase && !rawPassword.chars().anyMatch(Character::isLowerCase)) {
            errors.add("密码必须包含至少一个小写字母");
        }
        if (requireDigit && !rawPassword.chars().anyMatch(Character::isDigit)) {
            errors.add("密码必须包含至少一个数字");
        }
        if (requireSpecial && rawPassword.chars().allMatch(c -> Character.isLetterOrDigit(c))) {
            errors.add("密码必须包含至少一个特殊字符");
        }

        return errors;
    }

    @Override
    public boolean isHistoryPassword(Long userId, String rawPassword) {
        int historyCount = getIntConfig("pwd.history.count", 5);

        LambdaQueryWrapper<SysPasswordHistory> wrapper = new LambdaQueryWrapper<SysPasswordHistory>()
                .eq(SysPasswordHistory::getUserId, userId)
                .orderByDesc(SysPasswordHistory::getCreatedTime)
                .last("LIMIT " + historyCount);

        List<SysPasswordHistory> histories = historyMapper.selectList(wrapper);

        for (SysPasswordHistory history : histories) {
            if (passwordEncoder.matches(rawPassword, history.getPassword())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void recordHistory(Long userId, String encodedPassword) {
        SysPasswordHistory history = new SysPasswordHistory();
        history.setUserId(userId);
        history.setPassword(encodedPassword);
        history.setCreatedTime(LocalDateTime.now());
        historyMapper.insert(history);
    }

    @Override
    public boolean isExpired(SysUser user) {
        int expireDays = getIntConfig("pwd.expire.days", 0);
        if (expireDays <= 0) {
            return false;
        }

        LocalDateTime changedTime = user.getPasswordChangedTime();
        if (changedTime == null) {
            return true;
        }

        return changedTime.plusDays(expireDays).isBefore(LocalDateTime.now());
    }

    private int getIntConfig(String key, int defaultValue) {
        String value = configService.getValueByKey(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private boolean getBoolConfig(String key, boolean defaultValue) {
        String value = configService.getValueByKey(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
