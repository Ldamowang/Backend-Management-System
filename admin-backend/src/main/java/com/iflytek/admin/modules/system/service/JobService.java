package com.iflytek.admin.modules.system.service;

import com.iflytek.admin.common.result.PageResult;
import com.iflytek.admin.modules.system.dto.JobFormDTO;
import com.iflytek.admin.modules.system.entity.SysJob;
import com.iflytek.admin.modules.system.entity.SysJobLog;

public interface JobService {
    PageResult<SysJob> page(int page, int size);
    SysJob getById(Long id);
    void create(JobFormDTO dto);
    void update(Long id, JobFormDTO dto);
    void delete(Long id);
    void changeStatus(Long id, Integer status);
    void run(Long id);
    PageResult<SysJobLog> logPage(Long jobId, int page, int size);
}
