package org.example.certificatemanagesystem.common.vo;

public record LoginUserInfoVO(
        Long id,
        String username,
        String realName,
        String role,
        String token
){}

