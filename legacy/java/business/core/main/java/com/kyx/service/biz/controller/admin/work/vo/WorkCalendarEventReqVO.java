package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "Admin - Work calendar event request")
@Data
public class WorkCalendarEventReqVO {

    @Schema(description = "Start date", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-01-01")
    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate startDate;

    @Schema(description = "End date", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-01-31")
    @NotNull(message = "End date is required")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate endDate;

    @Schema(description = "Event types", example = "schedule,meeting,task,trip,leave")
    private List<String> types;
}
