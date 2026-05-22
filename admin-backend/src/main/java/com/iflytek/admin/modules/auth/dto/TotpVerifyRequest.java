package com.iflytek.admin.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TotpVerifyRequest {
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6,8}$", message = "验证码格式错误，需为6-8位数字")
    private String code;
}
