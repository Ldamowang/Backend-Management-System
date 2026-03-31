package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.common.result.ResultCode;
import com.iflytek.admin.modules.system.dto.UserCreateDTO;
import com.iflytek.admin.modules.system.dto.UserQueryDTO;
import com.iflytek.admin.modules.system.dto.UserUpdateDTO;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.entity.SysUserRole;
import com.iflytek.admin.modules.system.mapper.SysUserMapper;
import com.iflytek.admin.modules.system.mapper.SysUserRoleMapper;
import com.iflytek.admin.modules.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<Map<String, Object>> page(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysUser::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getEmail())) {
            wrapper.like(SysUser::getEmail, query.getEmail());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysUser::getCreatedTime);

        Page<SysUser> page = userMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream()
                .map(this::toUserMap)
                .collect(Collectors.toList());

        return new PageResult<>(list, page.getTotal(), query.getPage(), query.getSize());
    }

    @Override
    public Map<String, Object> getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        return toUserMap(user);
    }

    @Override
    @Transactional
    public void create(UserCreateDTO dto) {
        SysUser exists = userMapper.selectUserByUsername(dto.getUsername());
        if (exists != null) throw new BusinessException(ResultCode.USERNAME_EXISTS);

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());
        user.setStatus(dto.getStatus());
        userMapper.insert(user);

        saveUserRoles(user.getId(), dto.getRoleIds());
    }

    @Override
    @Transactional
    public void update(Long id, UserUpdateDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        userMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            saveUserRoles(id, dto.getRoleIds());
        }
    }

    @Override
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private static final int EXPORT_MAX_ROWS = 10000;

    @Override
    public List<Map<String, Object>> listForExport(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysUser::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getEmail())) {
            wrapper.like(SysUser::getEmail, query.getEmail());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysUser::getCreatedTime);
        wrapper.last("LIMIT " + EXPORT_MAX_ROWS);

        List<SysUser> users = userMapper.selectList(wrapper);
        return users.stream().map(this::toUserMap).collect(Collectors.toList());
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds != null) {
            roleIds.forEach(roleId -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            });
        }
    }

    private Map<String, Object> toUserMap(SysUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("avatar", user.getAvatar());
        map.put("gender", user.getGender());
        map.put("status", user.getStatus());
        map.put("createdTime", user.getCreatedTime());
        List<String> roles = userMapper.selectUserRoles(user.getId());
        map.put("roles", roles);
        return map;
    }
}
