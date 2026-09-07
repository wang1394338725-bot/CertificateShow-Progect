package org.example.certificatemanagesystem.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CertificateUpdateDTO {
    private Long id;
    private String title;
    private String recipient;
    private String eventName;
    private String awardLevel;
    private String projectName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate awardDate;
}
