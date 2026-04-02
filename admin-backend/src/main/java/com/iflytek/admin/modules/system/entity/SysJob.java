package com.iflytek.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iflytek.admin.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
public class SysJob extends BaseEntity {
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private Integer status;
    private String description;
}
