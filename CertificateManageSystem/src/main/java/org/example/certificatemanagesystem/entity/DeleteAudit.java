package org.example.certificatemanagesystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("delete_audit")
public class DeleteAudit {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long certificateId;
    private String certificateTitle;  // 提交时的标题快照，奖状被物理删除后消息仍可显示
    private Long requesterId;
    private String requesterName;     // 提交人姓名快照，提交人账号被删除后消息仍可显示
    private String reason;
    private Integer status;
    private Long approverId;
    private LocalDateTime approveTime;
    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
