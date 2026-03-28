package com.iflytek.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iflytek.admin.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    private String roleName;
    private String roleKey;
    private Integer sortOrder;
    private Integer status;
    private String description;
}
