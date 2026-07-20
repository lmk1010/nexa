package com.kyx.service.business.controller.admin.work.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "Admin - Work notice page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WorkNoticePageReqVO extends PageParam {

    @Schema(description = "Read status", example = "true")
    private Boolean readStatus;

    @Schema(description = "Created time range")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
}
