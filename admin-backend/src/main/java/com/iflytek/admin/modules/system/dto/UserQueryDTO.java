package com.iflytek.admin.modules.system.dto;

import com.iflytek.admin.base.PageQuery;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQuery {

    @Size(max = 50, message = "用户名最多50个字符")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Min(value = 0, message = "状态值无效")
    @Max(value = 1, message = "状态值无效")
    private Integer status;

    private Long deptId;
}
