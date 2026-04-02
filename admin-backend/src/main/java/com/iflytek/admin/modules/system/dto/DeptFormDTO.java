package com.iflytek.admin.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeptFormDTO {

    private Long parentId = 0L;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称最多50个字符")
    private String deptName;

    private Integer sortOrder = 0;

    @Size(max = 50, message = "负责人最多50个字符")
    private String leader;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer status = 1;
}
