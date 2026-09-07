package org.example.certificatemanagesystem.common.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理员列表视图对象：不暴露 password 字段 */
@Data
public class AdminVO {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private LocalDateTime createTime;
}
