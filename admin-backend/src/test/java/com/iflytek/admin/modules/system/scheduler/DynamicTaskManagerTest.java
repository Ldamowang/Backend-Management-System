package com.iflytek.admin.modules.system.scheduler;

import com.iflytek.admin.modules.system.entity.SysJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DynamicTaskManagerTest {

    private DynamicTaskManager taskManager;

    @Mock private ThreadPoolTaskScheduler taskScheduler;
    @Mock private JobRunner jobRunner;
    @Mock private ScheduledFuture<?> scheduledFuture;

    @BeforeEach
    void setUp() {
        taskManager = new DynamicTaskManager(taskScheduler, jobRunner);
    }

    private SysJob createJob(Long id, String cron) {
        SysJob job = new SysJob();
        job.setId(id);
        job.setJobName("测试任务");
        job.setInvokeTarget("testBean.method");
        job.setCronExpression(cron);
        return job;
    }

    @Nested
    @DisplayName("addTask")
    class AddTaskTests {
        @Test
        @DisplayName("注册 cron 任务到调度器")
        void addTask_registers_with_scheduler() {
            SysJob job = createJob(1L, "0/5 * * * * ?");
            doReturn(scheduledFuture)
                    .when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));

            taskManager.addTask(job);

            verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
            assertThat(taskManager.isRunning(1L)).isTrue();
        }

        @Test
        @DisplayName("无效 cron 表达式不注册")
        void addTask_invalid_cron_does_not_register() {
            SysJob job = createJob(2L, "invalid-cron");

            taskManager.addTask(job);

            verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
            assertThat(taskManager.isRunning(2L)).isFalse();
        }
    }

    @Nested
    @DisplayName("removeTask")
    class RemoveTaskTests {
        @Test
        @DisplayName("取消正在运行的任务")
        void removeTask_cancels_future() {
            SysJob job = createJob(1L, "0/5 * * * * ?");
            doReturn(scheduledFuture)
                    .when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
            taskManager.addTask(job);

            taskManager.removeTask(1L);

            verify(scheduledFuture).cancel(false);
            assertThat(taskManager.isRunning(1L)).isFalse();
        }

        @Test
        @DisplayName("移除不存在的任务不抛异常")
        void removeTask_nonexistent_no_error() {
            assertThatCode(() -> taskManager.removeTask(999L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("updateTask")
    class UpdateTaskTests {
        @Test
        @DisplayName("更新任务先取消再重新注册")
        void updateTask_removes_then_adds() {
            SysJob job = createJob(1L, "0/5 * * * * ?");
            doReturn(scheduledFuture)
                    .when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
            taskManager.addTask(job);

            SysJob updated = createJob(1L, "0/10 * * * * ?");
            taskManager.updateTask(updated);

            verify(scheduledFuture).cancel(false);
            verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(CronTrigger.class));
        }
    }
}
