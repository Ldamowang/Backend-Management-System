package com.iflytek.admin.modules.system.scheduler;

import com.iflytek.admin.common.annotation.ScheduledTarget;
import com.iflytek.admin.modules.system.entity.SysJob;
import com.iflytek.admin.modules.system.entity.SysJobLog;
import com.iflytek.admin.modules.system.mapper.SysJobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRunner {

    private final ApplicationContext applicationContext;
    private final SysJobLogMapper jobLogMapper;

    public void execute(SysJob job) {
        long startTime = System.currentTimeMillis();
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobId(job.getId());
        jobLog.setJobName(job.getJobName());
        jobLog.setInvokeTarget(job.getInvokeTarget());

        try {
            String invokeTarget = job.getInvokeTarget();
            int lastDot = invokeTarget.lastIndexOf('.');
            String beanName = invokeTarget.substring(0, lastDot);
            String methodName = invokeTarget.substring(lastDot + 1);

            Object bean = applicationContext.getBean(beanName);
            // 仅支持无参方法，带参方法会抛出 NoSuchMethodException（参见 @ScheduledTarget 约束）
            Method method = bean.getClass().getMethod(methodName);

            if (!method.isAnnotationPresent(ScheduledTarget.class)) {
                throw new SecurityException(
                        "方法 " + invokeTarget + " 未标注 @ScheduledTarget，拒绝执行");
            }

            method.invoke(bean);

            jobLog.setStatus(1);
            jobLog.setMessage("执行成功");
        } catch (Exception e) {
            log.error("任务执行失败: {}", job.getInvokeTarget(), e);
            jobLog.setStatus(0);
            String errorMsg = e.getMessage();
            if (errorMsg == null && e.getCause() != null) {
                errorMsg = e.getCause().getMessage();
            }
            jobLog.setErrorMsg(errorMsg != null ? errorMsg : "未知错误");
        } finally {
            jobLog.setDuration(System.currentTimeMillis() - startTime);
            jobLog.setCreatedTime(LocalDateTime.now());
            jobLogMapper.insert(jobLog);
        }
    }
}
