package com.kyx.service.hr.controller.admin.selfservice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - Employee self-service application Response VO")
@Data
public class HrSelfServiceApplicationRespVO {

    @Schema(description = "Business type")
    private String businessType;

    @Schema(description = "Business id")
    private Long businessId;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Summary")
    private String summary;

    @Schema(description = "Normalized status")
    private String status;

    @Schema(description = "Status text")
    private String statusText;

    @Schema(description = "Route path")
    private String routePath;

    @Schema(description = "Apply time")
    private LocalDateTime applyTime;

    @Schema(description = "Start time")
    private LocalDateTime startTime;

    @Schema(description = "End time")
    private LocalDateTime endTime;

    @Schema(description = "Profile id")
    private Long profileId;

    @Schema(description = "Profile name")
    private String profileName;

}
