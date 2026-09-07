package org.example.certificatemanagesystem.common.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CertificateVO {
    private Long id;
    private String title;
    private String recipient;
    private String eventName;
    private String awardLevel;
    private String projectName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate awardDate;

    @JsonProperty("isPinned")
    private Boolean isPinned;

    private Integer status;
    private String imageUrl;
}
