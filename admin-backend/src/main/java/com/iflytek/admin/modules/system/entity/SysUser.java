package com.iflytek.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iflytek.admin.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String username;
    private String password;
    private String nickname;
    private Long deptId;
    private String email;
    private String phone;
    private String avatar;
    private Integer gender;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime passwordChangedTime;
}
