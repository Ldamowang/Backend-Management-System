package com.iflytek.admin.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.common.result.Result;
import com.iflytek.admin.modules.system.entity.SysLoginLog;
import com.iflytek.admin.modules.system.mapper.SysLoginLogMapper;
import com.iflytek.admin.modules.system.mapper.SysMenuMapper;
import com.iflytek.admin.modules.system.mapper.SysRoleMapper;
import com.iflytek.admin.modules.system.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysLoginLogMapper loginLogMapper;

    @Operation(summary = "仪表盘统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userMapper.selectCount(null));
        stats.put("roleCount", roleMapper.selectCount(null));
        stats.put("menuCount", menuMapper.selectCount(null));
        stats.put("totalLoginCount", loginLogMapper.selectCount(null));

        // 今日登录次数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<SysLoginLog> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(SysLoginLog::getLoginTime, todayStart);
        stats.put("todayLoginCount", loginLogMapper.selectCount(todayWrapper));

        return Result.ok(stats);
    }
}
