package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - HR 生命周期发起离职 Request VO")
@Data
public class HrLifecycleResignationCreateReqVO {

    @Schema(description = "任职记录 ID", required = true)
    @NotNull(message = "任职记录 ID 不能为空")
    private Long entryId;

    @Schema(description = "离职日期", required = true)
    @NotNull(message = "离职日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate leaveDate;

    @Schema(description = "离职原因")
    private String leaveReason;

    @Schema(description = "交接人用户 ID")
    private Long handoverUserId;

    @Schema(description = "交接人姓名")
    private String handoverUserName;

    @Schema(description = "备注")
    private String remark;

}
