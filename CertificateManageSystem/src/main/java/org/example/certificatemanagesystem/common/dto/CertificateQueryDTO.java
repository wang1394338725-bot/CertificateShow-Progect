package org.example.certificatemanagesystem.common.dto;

import lombok.Data;

@Data
public class CertificateQueryDTO {
    private Integer current;  // 当前页码
    private Integer size;  // 每页条数
    private String keyword;  // 关键词
    private String level;  // 级别筛选
    private String status;  // 状态筛选
    private String sortBy;  // 排序方式：level=按等级（国家>省>市>校>其他），time=按获奖日期；空=默认置顶优先
}
