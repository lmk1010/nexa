package com.kyx.service.hr.controller.admin.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Schema(description = "DingTalk sync history response")
@Data
public class DingTalkSyncHistoryRespVO {

    @Schema(description = "Record id")
    private Long id;

    @Schema(description = "Sync type")
    private String syncType;

    @Schema(description = "Sync scope")
    private String syncScope;

    @Schema(description = "Trigger mode")
    private String triggerMode;

    @Schema(description = "Target tenant id")
    private Long targetTenantId;

    @Schema(description = "Operator user id")
    private Long operatorUserId;

    @Schema(description = "Attendance lookback minutes")
    private Long lookbackMinutes;

    @Schema(description = "Auto create enabled")
    private Boolean autoCreateEnabled;

    @Schema(description = "Auto create department id")
    private Long autoCreateDeptId;

    @Schema(description = "Total count")
    private Integer totalCount;

    @Schema(description = "Pulled count")
    private Integer pulledCount;

    @Schema(description = "Synced count")
    private Integer syncedCount;

    @Schema(description = "Created count")
    private Integer createdCount;

    @Schema(description = "Updated count")
    private Integer updatedCount;

    @Schema(description = "Failed count")
    private Integer failedCount;

    @Schema(description = "Skipped count")
    private Integer skippedCount;

    @Schema(description = "Sync start time")
    private LocalDateTime syncStartTime;

    @Schema(description = "Sync end time")
    private LocalDateTime syncEndTime;

    @Schema(description = "Duration in milliseconds")
    private Long durationMs;

    @Schema(description = "Summary")
    private String summary;

    @Schema(description = "Detail json")
    private String detailJson;

    @Schema(description = "Create time")
    private Date createTime;
}
