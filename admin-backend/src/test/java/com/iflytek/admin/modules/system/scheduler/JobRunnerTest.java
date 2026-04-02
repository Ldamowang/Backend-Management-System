package com.iflytek.admin.modules.system.scheduler;

import com.iflytek.admin.common.annotation.ScheduledTarget;
import com.iflytek.admin.modules.system.entity.SysJob;
import com.iflytek.admin.modules.system.entity.SysJobLog;
import com.iflytek.admin.modules.system.mapper.SysJobLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobRunnerTest {

    private JobRunner jobRunner;

    @Mock private ApplicationContext applicationContext;
    @Mock private SysJobLogMapper jobLogMapper;

    @BeforeEach
    void setUp() {
        jobRunner = new JobRunner(applicationContext, jobLogMapper);
    }

    // Test target bean
    public static class TestTaskBean {
        public boolean executed = false;

        @ScheduledTarget
        public void doWork() { executed = true; }

        public void unsafeMethod() { /* no annotation */ }
    }

    @Nested
    @DisplayName("执行成功")
    class SuccessTests {
        @Test
        @DisplayName("调用标注 @ScheduledTarget 的方法并记录成功日志")
        void execute_annotated_method_success() {
            SysJob job = new SysJob();
            job.setId(1L);
            job.setJobName("测试任务");
            job.setInvokeTarget("testTaskBean.doWork");

            TestTaskBean bean = new TestTaskBean();
            when(applicationContext.getBean("testTaskBean")).thenReturn(bean);

            jobRunner.execute(job);

            assertThat(bean.executed).isTrue();

            ArgumentCaptor<SysJobLog> logCaptor = ArgumentCaptor.forClass(SysJobLog.class);
            verify(jobLogMapper).insert(logCaptor.capture());
            SysJobLog log = logCaptor.getValue();
            assertThat(log.getJobId()).isEqualTo(1L);
            assertThat(log.getStatus()).isEqualTo(1);
            assertThat(log.getDuration()).isNotNull();
        }
    }

    @Nested
    @DisplayName("安全限制")
    class SecurityTests {
        @Test
        @DisplayName("拒绝调用未标注 @ScheduledTarget 的方法")
        void reject_unannotated_method() {
            SysJob job = new SysJob();
            job.setId(2L);
            job.setJobName("危险任务");
            job.setInvokeTarget("testTaskBean.unsafeMethod");

            TestTaskBean bean = new TestTaskBean();
            when(applicationContext.getBean("testTaskBean")).thenReturn(bean);

            jobRunner.execute(job);

            ArgumentCaptor<SysJobLog> logCaptor = ArgumentCaptor.forClass(SysJobLog.class);
            verify(jobLogMapper).insert(logCaptor.capture());
            SysJobLog log = logCaptor.getValue();
            assertThat(log.getStatus()).isEqualTo(0);
            assertThat(log.getErrorMsg()).contains("@ScheduledTarget");
        }
    }

    @Nested
    @DisplayName("异常处理")
    class ErrorTests {
        @Test
        @DisplayName("Bean 不存在时记录失败日志")
        void bean_not_found_logs_error() {
            SysJob job = new SysJob();
            job.setId(3L);
            job.setJobName("不存在的任务");
            job.setInvokeTarget("nonExistent.method");

            when(applicationContext.getBean("nonExistent"))
                    .thenThrow(new RuntimeException("No bean named 'nonExistent'"));

            jobRunner.execute(job);

            ArgumentCaptor<SysJobLog> logCaptor = ArgumentCaptor.forClass(SysJobLog.class);
            verify(jobLogMapper).insert(logCaptor.capture());
            assertThat(logCaptor.getValue().getStatus()).isEqualTo(0);
        }
    }
}
