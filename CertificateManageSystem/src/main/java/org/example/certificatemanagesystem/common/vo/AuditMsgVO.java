package org.example.certificatemanagesystem.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditMsgVO {
    private Long id;
    private Long certificateId;
    private String certificateTitle;
    private Long requesterId;
    private String requesterName;
    private String reason;
    private Integer status; // 0-待审核 1-已通过 2-已驳回 3-超管直接删除
    private String approverName;
    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
