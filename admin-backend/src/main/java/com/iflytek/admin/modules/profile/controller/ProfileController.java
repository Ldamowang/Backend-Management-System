package com.iflytek.admin.modules.profile.controller;

import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.common.utils.SecurityUtil;
import com.iflytek.admin.modules.profile.dto.PasswordUpdateDTO;
import com.iflytek.admin.modules.profile.dto.ProfileUpdateDTO;
import com.iflytek.admin.modules.system.entity.SysUser;
import com.iflytek.admin.modules.system.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "获取个人信息")
    @GetMapping
    public Result<Map<String, Object>> getProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("avatar", user.getAvatar());
        profile.put("gender", user.getGender());
        return Result.ok(profile);
    }

    @Operation(summary = "更新个人信息")
    @PutMapping
    public Result<Void> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        userMapper.updateById(user);
        return Result.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "当前密码错误");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        return Result.ok();
    }
}
