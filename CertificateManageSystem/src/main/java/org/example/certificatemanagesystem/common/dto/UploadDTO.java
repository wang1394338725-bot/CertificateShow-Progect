package org.example.certificatemanagesystem.common.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UploadDTO {
    private String title;
    private String recipient;
    private String eventName;
    private String awardLevel;
    private String projectName;
    private LocalDate awardDate;
}
