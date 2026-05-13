package com.iflytek.admin.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TotpVerifyRequest {
    @NotBlank(message = "验证码不能为空")
    private String code;
}
