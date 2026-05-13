package com.iflytek.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iflytek.admin.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_2fa")
public class SysUser2fa extends BaseEntity {
    private Long userId;
    private String secretKey;
    private Integer enabled;
    private String backupCodes;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
