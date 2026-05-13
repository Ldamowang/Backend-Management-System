package com.iflytek.admin.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.common.constant.CacheConstants;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.ResultCode;
import com.iflytek.admin.common.service.CacheService;
import com.iflytek.admin.common.utils.JwtUtil;
import com.iflytek.admin.modules.auth.dto.LoginRequest;
import com.iflytek.admin.modules.auth.dto.LoginResponse;
import com.iflytek.admin.modules.auth.service.AuthService;
import com.iflytek.admin.modules.auth.service.TotpService;
import com.iflytek.admin.modules.system.entity.*;
import com.iflytek.admin.modules.system.mapper.*;
import com.iflytek.admin.modules.system.service.OnlineUserService;
import com.iflytek.admin.modules.system.service.PasswordPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheService cacheService;
    private final PasswordPolicyService passwordPolicyService;
    private final OnlineUserService onlineUserService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TotpService totpService;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectUserByUsername(request.getUsername());
        if (user == null) {
            saveLoginLog(request.getUsername(), 0, "用户不存在");
            throw new BusinessException(ResultCode.WRONG_PASSWORD);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            saveLoginLog(request.getUsername(), 0, "密码错误");
            throw new BusinessException(ResultCode.WRONG_PASSWORD);
        }

        if (user.getStatus() == 0) {
            saveLoginLog(request.getUsername(), 0, "用户已禁用");
            throw new BusinessException(40006, "用户已被禁用");
        }

        // 2FA 校验
        if (totpService.isEnabled(user.getId())) {
            if (request.getTotpCode() == null || request.getTotpCode().isBlank()) {
                // 需要 2FA 但未提供验证码
                return LoginResponse.builder()
                        .requiresTwoFactor(true)
                        .build();
            }
            if (!totpService.validateForLogin(user.getId(), request.getTotpCode())) {
                saveLoginLog(request.getUsername(), 0, "2FA验证码错误");
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "2FA验证码错误");
            }
        }

        // 更新登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        saveLoginLog(request.getUsername(), 1, "登录成功");

        // 并发登录控制
        int maxSessions = getConfigInt("login.max-sessions", CacheConstants.DEFAULT_MAX_SESSIONS);
        String conflictPolicy = getConfigString("login.conflict-policy", "kick_old");

        long currentSessions = onlineUserService.getSessionCount(user.getId());
        if (currentSessions >= maxSessions) {
            if ("reject_new".equals(conflictPolicy)) {
                saveLoginLog(request.getUsername(), 0, "并发会话数超限，拒绝登录");
                throw new BusinessException(40007, "当前账号已在其他设备登录，不允许重复登录");
            }
            // kick_old: 踢掉最旧的会话直到低于限制
            while (onlineUserService.getSessionCount(user.getId()) >= maxSessions) {
                String kickedToken = onlineUserService.kickOldestSession(user.getId());
                if (kickedToken == null) break;
                // 通过 WebSocket 通知被踢用户
                notifyKickedUser(user.getId(), kickedToken);
            }
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 记录在线用户（使用 Sorted Set 管理会话）
        Map<String, Object> onlineInfo = new HashMap<>();
        onlineInfo.put("userId", user.getId());
        onlineInfo.put("username", user.getUsername());
        onlineInfo.put("nickname", user.getNickname());
        onlineInfo.put("loginTime", LocalDateTime.now().toString());
        onlineUserService.addSession(user.getId(), accessToken, onlineInfo);

        boolean passwordExpired = passwordPolicyService.isExpired(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                .passwordExpired(passwordExpired)
                .build();
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) {
            // 使用 onlineUserService 统一移除会话（含 Sorted Set 清理）
            onlineUserService.kickOut(token);
        }
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
        // 校验 token 类型必须为 refresh，防止 access token 被滥用为刷新令牌
        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(userId, username);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                .build();
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);

        List<String> roles = userMapper.selectUserRoles(userId);

        // 从缓存获取权限列表
        String permCacheKey = CacheConstants.PERM_USER_PREFIX + userId;
        List<String> permissions = cacheService.getAsList(permCacheKey);
        if (permissions == null) {
            permissions = userMapper.selectUserPermissions(userId);
            cacheService.set(permCacheKey, permissions, CacheConstants.MENU_PERM_TTL);
        }

        // 获取用户角色ID列表
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        // 从缓存获取菜单树
        String menuCacheKey = CacheConstants.MENU_USER_PREFIX + userId;
        List<Map<String, Object>> menuTree = cacheService.getAsList(menuCacheKey);
        if (menuTree == null) {
            List<SysMenu> menus = roleIds.isEmpty() ? List.of() : menuMapper.selectMenusByRoleIds(roleIds);
            menuTree = buildMenuTree(menus, 0L);
            cacheService.set(menuCacheKey, menuTree, CacheConstants.MENU_PERM_TTL);
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("email", user.getEmail());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("gender", user.getGender());

        Map<String, Object> result = new HashMap<>();
        result.put("user", userInfo);
        result.put("roles", roles);
        result.put("permissions", permissions);
        result.put("menus", menuTree);
        return result;
    }

    private List<Map<String, Object>> buildMenuTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .map(m -> {
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", m.getId());
                    node.put("parentId", m.getParentId());
                    node.put("menuName", m.getMenuName());
                    node.put("menuType", m.getMenuType());
                    node.put("path", m.getPath());
                    node.put("component", m.getComponent());
                    node.put("icon", m.getIcon());
                    node.put("sortOrder", m.getSortOrder());
                    node.put("permission", m.getPermission());
                    node.put("visible", m.getVisible());
                    node.put("children", buildMenuTree(menus, m.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }

    private int getConfigInt(String key, int defaultValue) {
        try {
            Object val = cacheService.get(CacheConstants.CONFIG_PREFIX + key, String.class);
            if (val != null) return Integer.parseInt(val.toString());
        } catch (Exception ignored) {}
        return defaultValue;
    }

    private String getConfigString(String key, String defaultValue) {
        try {
            Object val = cacheService.get(CacheConstants.CONFIG_PREFIX + key, String.class);
            if (val != null) return val.toString();
        } catch (Exception ignored) {}
        return defaultValue;
    }

    private void notifyKickedUser(Long userId, String kickedToken) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", "您的账号在其他设备登录，当前会话已被强制下线");
            payload.put("kickedToken", kickedToken);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(), "/queue/kick", payload);
        } catch (Exception e) {
            log.warn("发送踢人通知失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    private void saveLoginLog(String username, int status, String message) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setStatus(status);
        log.setMessage(message);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}
