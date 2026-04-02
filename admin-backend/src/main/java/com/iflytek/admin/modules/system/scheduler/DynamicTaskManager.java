package com.iflytek.admin.modules.system.scheduler;

import com.iflytek.admin.modules.system.entity.SysJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicTaskManager {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final JobRunner jobRunner;
    private final Map<Long, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

    public void addTask(SysJob job) {
        try {
            CronTrigger trigger = new CronTrigger(job.getCronExpression());
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> jobRunner.execute(job), trigger);
            runningTasks.put(job.getId(), future);
            log.info("任务已注册: id={}, name={}, cron={}", job.getId(), job.getJobName(), job.getCronExpression());
        } catch (IllegalArgumentException e) {
            log.error("无效的 cron 表达式: job={}, cron={}", job.getId(), job.getCronExpression(), e);
        }
    }

    public void removeTask(Long jobId) {
        ScheduledFuture<?> future = runningTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("任务已取消: id={}", jobId);
        }
    }

    public void updateTask(SysJob job) {
        removeTask(job.getId());
        addTask(job);
    }

    public boolean isRunning(Long jobId) {
        return runningTasks.containsKey(jobId);
    }
}
