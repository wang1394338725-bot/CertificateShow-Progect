package org.example.certificatemanagesystem.common.enums;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {

    // 1. 成功
    SUCCESS(200, "操作成功"),

    // 2. 客户端错误 (400-499)
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "账号未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问该资源"),
    NOT_FOUND(404, "请求资源不存在"),
    USER_ERROR(405,"用户名或密码错误"),

    // 3. 服务端错误 (500-599)
    INTERNAL_SERVER_ERROR(500, "系统繁忙，请稍后重试");

    // 字段定义
    private final Integer code;
    private final String message;

    // 构造方法（枚举的构造方法默认是 private，可以不写修饰符）
    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

