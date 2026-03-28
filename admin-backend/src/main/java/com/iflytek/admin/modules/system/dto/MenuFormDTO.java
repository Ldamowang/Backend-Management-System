package com.iflytek.admin.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuFormDTO {

    private Long parentId = 0L;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称最多50个字符")
    private String menuName;

    @NotNull(message = "菜单类型不能为空")
    private Integer menuType;

    @Size(max = 200, message = "路由路径最多200个字符")
    private String path;

    @Size(max = 200, message = "组件路径最多200个字符")
    private String component;

    @Size(max = 100, message = "图标最多100个字符")
    private String icon;

    private Integer sortOrder = 0;

    @Size(max = 100, message = "权限标识最多100个字符")
    private String permission;

    private Integer visible = 1;

    private Integer status = 1;
}
