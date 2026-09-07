package org.example.certificatemanagesystem.common.dto;

import lombok.Data;

@Data
public class CertificateQueryDTO {
    private Integer current;  // 当前页码
    private Integer size;  // 每页条数
    private String keyword;  // 关键词
    private String level;  // 级别筛选
    private String status;  // 状态筛选
}
