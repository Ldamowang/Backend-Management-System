package com.iflytek.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_job_log")
public class SysJobLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private String jobName;
    private String invokeTarget;
    private Integer status;
    private String message;
    private String errorMsg;
    private Long duration;
    private LocalDateTime createdTime;
}
