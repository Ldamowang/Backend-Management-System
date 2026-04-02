package com.iflytek.admin.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iflytek.admin.common.exception.BusinessException;
import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.modules.system.dto.JobFormDTO;
import com.iflytek.admin.modules.system.entity.SysJob;
import com.iflytek.admin.modules.system.entity.SysJobLog;
import com.iflytek.admin.modules.system.mapper.SysJobLogMapper;
import com.iflytek.admin.modules.system.mapper.SysJobMapper;
import com.iflytek.admin.modules.system.scheduler.DynamicTaskManager;
import com.iflytek.admin.modules.system.scheduler.JobRunner;
import com.iflytek.admin.modules.system.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final SysJobMapper jobMapper;
    private final SysJobLogMapper jobLogMapper;
    private final DynamicTaskManager taskManager;
    private final JobRunner jobRunner;

    @Override
    public PageResult<SysJob> page(int page, int size) {
        Page<SysJob> result = jobMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysJob>().orderByDesc(SysJob::getCreatedTime));
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public SysJob getById(Long id) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) throw new BusinessException(404, "任务不存在");
        return job;
    }

    @Override
    public void create(JobFormDTO dto) {
        SysJob job = new SysJob();
        applyDTO(job, dto);
        jobMapper.insert(job);
        if (Integer.valueOf(1).equals(job.getStatus())) {
            taskManager.addTask(job);
        }
    }

    @Override
    public void update(Long id, JobFormDTO dto) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) throw new BusinessException(404, "任务不存在");
        applyDTO(job, dto);
        jobMapper.updateById(job);
        if (Integer.valueOf(1).equals(job.getStatus())) {
            taskManager.updateTask(job);
        } else {
            taskManager.removeTask(id);
        }
    }

    @Override
    public void delete(Long id) {
        taskManager.removeTask(id);
        jobMapper.deleteById(id);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) throw new BusinessException(404, "任务不存在");
        job.setStatus(status);
        jobMapper.updateById(job);
        if (Integer.valueOf(1).equals(status)) {
            taskManager.addTask(job);
        } else {
            taskManager.removeTask(id);
        }
    }

    @Override
    public void run(Long id) {
        SysJob job = jobMapper.selectById(id);
        if (job == null) throw new BusinessException(404, "任务不存在");
        jobRunner.execute(job);
    }

    @Override
    public PageResult<SysJobLog> logPage(Long jobId, int page, int size) {
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<>();
        if (jobId != null) {
            wrapper.eq(SysJobLog::getJobId, jobId);
        }
        wrapper.orderByDesc(SysJobLog::getCreatedTime);
        Page<SysJobLog> result = jobLogMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    private void applyDTO(SysJob job, JobFormDTO dto) {
        job.setJobName(dto.getJobName());
        job.setJobGroup(dto.getJobGroup());
        job.setInvokeTarget(dto.getInvokeTarget());
        job.setCronExpression(dto.getCronExpression());
        job.setStatus(dto.getStatus());
        job.setDescription(dto.getDescription());
    }

}
