package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - HR 生命周期发起调薪 Request VO")
@Data
public class HrLifecycleSalaryAdjustCreateReqVO {

    @Schema(description = "任职记录 ID", required = true)
    @NotNull(message = "任职记录 ID 不能为空")
    private Long entryId;

    @Schema(description = "生效日期", required = true)
    @NotNull(message = "生效日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDate;

    @Schema(description = "薪酬类型")
    private String salaryType;

    @Schema(description = "调整后金额", required = true)
    @NotNull(message = "调整后金额不能为空")
    private BigDecimal amount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "调薪原因")
    private String reason;

    @Schema(description = "备注")
    private String remark;

}
