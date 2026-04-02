package com.iflytek.admin.modules.system.controller;

import com.iflytek.admin.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.*;

@Tag(name = "系统监控")
@RestController
@RequestMapping("/api/server")
@RequiredArgsConstructor
public class ServerController {

    private final RedisConnectionFactory redisConnectionFactory;

    @Operation(summary = "服务器信息")
    @PreAuthorize("hasAuthority('sys:monitor:list')")
    @GetMapping("/info")
    public Result<Map<String, Object>> serverInfo() {
        Map<String, Object> result = new LinkedHashMap<>();

        // JVM 信息
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("javaVersion", System.getProperty("java.version"));
        jvm.put("javaVendor", System.getProperty("java.vendor"));
        jvm.put("jvmName", runtimeBean.getVmName());
        jvm.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024));
        jvm.put("heapMax", memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024));
        jvm.put("heapCommitted", memoryBean.getHeapMemoryUsage().getCommitted() / (1024 * 1024));
        jvm.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024));
        jvm.put("startTime", new Date(runtimeBean.getStartTime()));
        jvm.put("uptime", runtimeBean.getUptime() / 1000);
        result.put("jvm", jvm);

        // 操作系统信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> os = new LinkedHashMap<>();
        os.put("name", osBean.getName());
        os.put("arch", osBean.getArch());
        os.put("version", osBean.getVersion());
        os.put("availableProcessors", osBean.getAvailableProcessors());
        os.put("systemLoadAverage", osBean.getSystemLoadAverage());
        result.put("os", os);

        // 磁盘信息
        java.io.File root = new java.io.File("/");
        Map<String, Object> disk = new LinkedHashMap<>();
        disk.put("total", root.getTotalSpace() / (1024 * 1024 * 1024));
        disk.put("free", root.getFreeSpace() / (1024 * 1024 * 1024));
        disk.put("usable", root.getUsableSpace() / (1024 * 1024 * 1024));
        disk.put("used", (root.getTotalSpace() - root.getFreeSpace()) / (1024 * 1024 * 1024));
        result.put("disk", disk);

        // Redis 信息
        try {
            Properties redisInfo = redisConnectionFactory.getConnection().serverCommands().info();
            Map<String, Object> redis = new LinkedHashMap<>();
            if (redisInfo != null) {
                redis.put("version", redisInfo.getProperty("redis_version"));
                redis.put("usedMemory", redisInfo.getProperty("used_memory_human"));
                redis.put("connectedClients", redisInfo.getProperty("connected_clients"));
                redis.put("uptimeInDays", redisInfo.getProperty("uptime_in_days"));
                redis.put("dbSize", redisConnectionFactory.getConnection().serverCommands().dbSize());
            }
            result.put("redis", redis);
        } catch (Exception e) {
            result.put("redis", Map.of("error", "Redis 连接失败"));
        }

        return Result.ok(result);
    }
}
