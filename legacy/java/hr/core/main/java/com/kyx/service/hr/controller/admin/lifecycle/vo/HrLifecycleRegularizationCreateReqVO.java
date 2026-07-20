package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - HR 生命周期发起转正 Request VO")
@Data
public class HrLifecycleRegularizationCreateReqVO {

    @Schema(description = "任职记录 ID", required = true)
    @NotNull(message = "任职记录 ID 不能为空")
    private Long entryId;

    @Schema(description = "转正日期", required = true)
    @NotNull(message = "转正日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate confirmationDate;

    @Schema(description = "负责人评价")
    private String managerEvaluation;

    @Schema(description = "HR 复核意见")
    private String hrReview;

    @Schema(description = "备注")
    private String remark;

}
