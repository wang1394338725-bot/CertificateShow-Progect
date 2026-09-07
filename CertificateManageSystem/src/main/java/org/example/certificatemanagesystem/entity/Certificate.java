package org.example.certificatemanagesystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("certificate")
public class Certificate {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String recipient;
    private String awardLevel;
    private String eventName;
    private String projectName;
    private String organization;
    private LocalDate awardDate;

    @TableField("is_pinned")
    private Boolean isPinned;

    private Integer sortOrder;
    private Long viewCount;
    private Integer status;
    private String imageUrl;
    private String imageKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
