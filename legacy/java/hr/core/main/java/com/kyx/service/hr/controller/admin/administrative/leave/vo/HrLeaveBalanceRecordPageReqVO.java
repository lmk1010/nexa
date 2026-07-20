package com.kyx.service.hr.controller.admin.administrative.leave.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 假期余额流水分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrLeaveBalanceRecordPageReqVO extends PageParam {

    @Schema(description = "余额ID")
    private Long balanceId;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "假期类型编码")
    private String leaveTypeCode;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "变动类型")
    private String changeType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
