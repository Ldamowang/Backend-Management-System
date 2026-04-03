package com.iflytek.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    private Integer responseCode;
    private String responseResult;
    private String ip;
    private Long duration;
    private Integer status;
    private String errorMsg;
    private String targetType;
    private Long targetId;
    private String beforeData;
    private String afterData;
    private LocalDateTime createdTime;
}
