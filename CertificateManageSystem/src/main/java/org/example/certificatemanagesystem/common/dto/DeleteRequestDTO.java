package org.example.certificatemanagesystem.common.dto;

import lombok.Data;

@Data
public class DeleteRequestDTO {
    private Long certificateId;
    private String reason;
}
