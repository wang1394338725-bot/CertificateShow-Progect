package org.example.certificatemanagesystem.common.dto;

import lombok.Data;

@Data
public class AdminPageDTO {
    private Integer current = 1;
    private Integer size = 10;
    /** 按用户名/真实姓名模糊搜索，可为空 */
    private String keyword;
}
