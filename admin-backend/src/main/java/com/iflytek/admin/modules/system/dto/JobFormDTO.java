package com.iflytek.admin.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobFormDTO {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称最多100个字符")
    private String jobName;

    @Size(max = 50, message = "任务分组最多50个字符")
    private String jobGroup = "DEFAULT";

    @NotBlank(message = "调用目标不能为空")
    @Size(max = 200, message = "调用目标最多200个字符")
    private String invokeTarget;

    @NotBlank(message = "cron表达式不能为空")
    @Size(max = 100, message = "cron表达式最多100个字符")
    private String cronExpression;

    private Integer status = 1;

    @Size(max = 500, message = "描述最多500个字符")
    private String description;
}
