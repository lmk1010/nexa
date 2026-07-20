package com.kyx.service.biz.controller.admin.work.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "Admin - Work calendar event create/update request")
@Data
public class WorkCalendarEventSaveReqVO {

    @Schema(description = "Event ID", example = "1024")
    private Long id;

    @Schema(description = "Title", requiredMode = Schema.RequiredMode.REQUIRED, example = "Team sync")
    @NotBlank(message = "Title is required")
    @Size(max = 128, message = "Title cannot exceed 128 characters")
    private String title;

    @Schema(description = "Start time", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Start time is required")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime startTime;

    @Schema(description = "End time", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "End time is required")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;

    @Schema(description = "Location", example = "A501")
    @Size(max = 128, message = "Location cannot exceed 128 characters")
    private String location;

    @Schema(description = "Remark", example = "Prepare weekly report")
    @Size(max = 512, message = "Remark cannot exceed 512 characters")
    private String remark;
}
