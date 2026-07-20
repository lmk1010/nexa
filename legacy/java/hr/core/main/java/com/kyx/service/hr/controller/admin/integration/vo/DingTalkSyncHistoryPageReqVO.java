package com.kyx.service.hr.controller.admin.integration.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "DingTalk sync history page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DingTalkSyncHistoryPageReqVO extends PageParam {

    @Schema(description = "Sync type")
    private String syncType;

    @Schema(description = "Sync scope")
    private String syncScope;

    @Schema(description = "Trigger mode")
    private String triggerMode;

    @Schema(description = "Target tenant id")
    private Long targetTenantId;

    @Schema(description = "Sync finish time range")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] syncEndTime;
}
