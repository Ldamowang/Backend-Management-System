package com.iflytek.admin.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleFormDTO {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称最多50个字符")
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    @Size(max = 50, message = "角色标识最多50个字符")
    private String roleKey;

    private Integer sortOrder = 0;

    private Integer status = 1;

    @Size(max = 500, message = "描述最多500个字符")
    private String description;
}
