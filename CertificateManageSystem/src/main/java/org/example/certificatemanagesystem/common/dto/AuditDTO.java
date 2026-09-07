package org.example.certificatemanagesystem.common.dto;

import lombok.Data;

@Data
public class AuditDTO {
    private Long auditId;
    private Boolean approved;      // true-通过 false-驳回
    private String rejectReason;   // 驳回时必填
}
