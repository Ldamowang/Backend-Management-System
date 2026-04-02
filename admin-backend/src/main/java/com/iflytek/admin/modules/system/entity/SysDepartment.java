package com.iflytek.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iflytek.admin.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_department")
public class SysDepartment extends BaseEntity {
    private Long parentId;
    private String deptName;
    private Integer sortOrder;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
}
