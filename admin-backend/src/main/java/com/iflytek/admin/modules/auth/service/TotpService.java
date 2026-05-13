package com.iflytek.admin.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.ResultCode;
import com.iflytek.admin.modules.auth.dto.TotpSetupResponse;
import com.iflytek.admin.modules.system.entity.SysUser2fa;
import com.iflytek.admin.modules.system.mapper.SysUser2faMapper;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TotpService {
    private static final String ISSUER = "AdminSystem";
    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;

    private final SysUser2faMapper sysUser2faMapper;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public TotpSetupResponse generateSetup(Long userId, String username) {
        // 检查是否已存在
        SysUser2fa existing = sysUser2faMapper.selectOne(
            new LambdaQueryWrapper<SysUser2fa>().eq(SysUser2fa::getUserId, userId)
        );
        if (existing != null && existing.getEnabled() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "2FA已启用，请先禁用");
        }

        // 生成密钥
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secretKey = key.getKey();

        // 生成二维码 URI
        String qrCodeUri = String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            ISSUER, username, secretKey, ISSUER
        );

        // 生成备用码
        List<String> backupCodes = generateBackupCodes();

        // 保存到数据库（enabled=0）
        SysUser2fa user2fa = new SysUser2fa();
        user2fa.setUserId(userId);
        user2fa.setSecretKey(secretKey);
        user2fa.setEnabled(0);
        try {
            user2fa.setBackupCodes(objectMapper.writeValueAsString(backupCodes));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "备用码序列化失败");
        }
        user2fa.setCreatedTime(LocalDateTime.now());
        user2fa.setUpdatedTime(LocalDateTime.now());

        if (existing != null) {
            user2fa.setId(existing.getId());
            sysUser2faMapper.updateById(user2fa);
        } else {
            sysUser2faMapper.insert(user2fa);
        }

        return TotpSetupResponse.builder()
            .secretKey(secretKey)
            .qrCodeUri(qrCodeUri)
            .backupCodes(backupCodes)
            .build();
    }

    @Transactional
    public void verifyAndEnable(Long userId, String code) {
        SysUser2fa user2fa = sysUser2faMapper.selectOne(
            new LambdaQueryWrapper<SysUser2fa>().eq(SysUser2fa::getUserId, userId)
        );
        if (user2fa == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先生成2FA密钥");
        }

        if (!googleAuthenticator.authorize(user2fa.getSecretKey(), Integer.parseInt(code))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误");
        }

        user2fa.setEnabled(1);
        user2fa.setUpdatedTime(LocalDateTime.now());
        sysUser2faMapper.updateById(user2fa);
    }

    @Transactional
    public void disable(Long userId) {
        sysUser2faMapper.delete(
            new LambdaQueryWrapper<SysUser2fa>().eq(SysUser2fa::getUserId, userId)
        );
    }

    public boolean validateForLogin(Long userId, String totpCode) {
        SysUser2fa user2fa = sysUser2faMapper.selectOne(
            new LambdaQueryWrapper<SysUser2fa>()
                .eq(SysUser2fa::getUserId, userId)
                .eq(SysUser2fa::getEnabled, 1)
        );
        if (user2fa == null) {
            return false;
        }

        // 先尝试 TOTP 验证
        try {
            int code = Integer.parseInt(totpCode);
            if (googleAuthenticator.authorize(user2fa.getSecretKey(), code)) {
                return true;
            }
        } catch (NumberFormatException e) {
            // 不是数字，跳过 TOTP 验证
        }

        // 尝试备用码验证
        try {
            List<String> backupCodes = objectMapper.readValue(
                user2fa.getBackupCodes(),
                new TypeReference<List<String>>() {}
            );
            if (backupCodes.contains(totpCode)) {
                // 一次性消费备用码
                backupCodes.remove(totpCode);
                user2fa.setBackupCodes(objectMapper.writeValueAsString(backupCodes));
                user2fa.setUpdatedTime(LocalDateTime.now());
                sysUser2faMapper.updateById(user2fa);
                return true;
            }
        } catch (JsonProcessingException e) {
            log.error("备用码解析失败", e);
        }

        return false;
    }

    public boolean isEnabled(Long userId) {
        Long count = sysUser2faMapper.selectCount(
            new LambdaQueryWrapper<SysUser2fa>()
                .eq(SysUser2fa::getUserId, userId)
                .eq(SysUser2fa::getEnabled, 1)
        );
        return count > 0;
    }

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                code.append(secureRandom.nextInt(10));
            }
            codes.add(code.toString());
        }
        return codes;
    }
}

