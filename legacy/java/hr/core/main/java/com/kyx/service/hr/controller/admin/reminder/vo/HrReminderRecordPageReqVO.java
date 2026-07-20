package com.kyx.service.hr.controller.admin.reminder.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "Admin - HR reminder record page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrReminderRecordPageReqVO extends PageParam {

    @Schema(description = "Keyword")
    private String keyword;

    @Schema(description = "Source type")
    private String sourceType;

    @Schema(description = "Business type")
    private String businessType;

    @Schema(description = "Receiver user ID")
    private Long receiverUserId;

    @Schema(description = "Status")
    private String status;

    @Schema(description = "Severity")
    private String severity;

    @Schema(description = "Only mine")
    private Boolean mine;

    @Schema(description = "Trigger time range")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] triggerTime;

}
