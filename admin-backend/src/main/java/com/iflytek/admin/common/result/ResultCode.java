package com.iflytek.admin.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    USER_NOT_FOUND(40001, "用户不存在"),
    USERNAME_EXISTS(40002, "用户名已存在"),
    WRONG_PASSWORD(40003, "用户名或密码错误"),
    TOKEN_EXPIRED(40004, "Token已过期"),
    TOKEN_INVALID(40005, "Token无效"),
    RATE_LIMIT_EXCEEDED(429, "请求过于频繁，请稍后再试"),
    DUPLICATE_SUBMIT(409, "该操作已提交，请勿重复操作");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
