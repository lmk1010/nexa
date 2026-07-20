package com.kyx.service.biz.controller.admin.work.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "Admin - Work calendar event response")
@Data
public class WorkCalendarEventRespVO {

    @Schema(description = "Event id", example = "meeting-1024")
    private String id;

    @Schema(description = "Event title", example = "Team sync")
    private String title;

    @Schema(description = "Event type", example = "meeting")
    private String type;

    @Schema(description = "Start time")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime startTime;

    @Schema(description = "End time")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;

    @Schema(description = "Location", example = "A501")
    private String location;

    @Schema(description = "Owner", example = "HR")
    private String owner;

    @Schema(description = "Remark", example = "Prepare weekly report")
    private String remark;

    @Schema(description = "Route path", example = "/work/requirement/detail?id=1024")
    private String routePath;

    @Schema(description = "Whether the current user can edit this event", example = "true")
    private Boolean editable;
}
