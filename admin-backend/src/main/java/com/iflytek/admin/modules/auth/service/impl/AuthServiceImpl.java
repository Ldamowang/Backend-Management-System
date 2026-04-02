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
import com.iflytek.admin.modules.system.entity.*;
import com.iflytek.admin.modules.system.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

        // 更新登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        saveLoginLog(request.getUsername(), 1, "登录成功");

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        // 记录在线用户
        Map<String, Object> onlineInfo = new HashMap<>();
        onlineInfo.put("userId", user.getId());
        onlineInfo.put("username", user.getUsername());
        onlineInfo.put("nickname", user.getNickname());
        onlineInfo.put("loginTime", LocalDateTime.now().toString());
        redisTemplate.opsForValue().set(
                CacheConstants.ONLINE_USER_PREFIX + accessToken,
                onlineInfo,
                jwtUtil.getAccessTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                .build();
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null) {
            redisTemplate.opsForValue().set(
                    CacheConstants.TOKEN_BLACKLIST_PREFIX + token,
                    "1",
                    jwtUtil.getAccessTokenExpiration(),
                    TimeUnit.MILLISECONDS
            );
            // 移除在线用户记录
            redisTemplate.delete(CacheConstants.ONLINE_USER_PREFIX + token);
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

    private void saveLoginLog(String username, int status, String message) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setStatus(status);
        log.setMessage(message);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}
