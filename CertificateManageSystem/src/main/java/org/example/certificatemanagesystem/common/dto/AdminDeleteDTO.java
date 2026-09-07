package org.example.certificatemanagesystem.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminDeleteDTO {
    @NotNull(message = "管理员ID不能为空")
    private Long id;
}
