package com.iflytek.admin.modules.system.dto;

import com.iflytek.admin.common.annotation.Exportable;
import lombok.Data;

@Data
public class UserExportDTO {
    @Exportable(value = "用户名", order = 1, required = true)
    private String username;

    @Exportable(value = "昵称", order = 2, required = true)
    private String nickname;

    @Exportable(value = "邮箱", order = 3)
    private String email;

    @Exportable(value = "手机号", order = 4)
    private String phone;

    @Exportable(value = "性别(0女1男2未知)", order = 5)
    private Integer gender;

    @Exportable(value = "状态(0禁用1正常)", order = 6, importable = false)
    private Integer status;
}
