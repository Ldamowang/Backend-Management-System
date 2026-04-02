package com.iflytek.admin.modules.system.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iflytek.admin.modules.system.entity.SysJob;
import com.iflytek.admin.modules.system.mapper.SysJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobInitializer implements ApplicationRunner {

    private final SysJobMapper jobMapper;
    private final DynamicTaskManager taskManager;

    @Override
    public void run(ApplicationArguments args) {
        List<SysJob> activeJobs = jobMapper.selectList(
                new LambdaQueryWrapper<SysJob>().eq(SysJob::getStatus, 1));
        log.info("启动加载定时任务: 共 {} 个", activeJobs.size());
        activeJobs.forEach(taskManager::addTask);
    }
}
