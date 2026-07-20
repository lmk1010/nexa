package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - HR 生命周期发起调岗 Request VO")
@Data
public class HrLifecycleTransferCreateReqVO {

    @Schema(description = "任职记录 ID", required = true)
    @NotNull(message = "任职记录 ID 不能为空")
    private Long entryId;

    @Schema(description = "生效日期", required = true)
    @NotNull(message = "生效日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDate;

    @Schema(description = "调入部门 ID")
    private Long targetDeptId;

    @Schema(description = "调入职位")
    private String targetJobTitle;

    @Schema(description = "调入职级 ID")
    private Long targetJobLevelId;

    @Schema(description = "调入序列 ID")
    private Long targetJobSequenceId;

    @Schema(description = "调入工作地点 ID")
    private Long targetWorkLocationId;

    @Schema(description = "新直属上级 ID")
    private Long targetDirectSupervisorId;

    @Schema(description = "调岗原因")
    private String reason;

    @Schema(description = "备注")
    private String remark;

}
