package com.kyx.service.finance.controller.admin.closing.vo;

import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.finance.enums.FinanceClosingTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 管理后台 - 执行月末结账 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 执行月末结账 Request VO")
public class FinanceClosingExecuteReqVO {

    @Schema(description = "账套ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套ID不能为空")
    private Long companyId;

    @Schema(description = "结账期间 yyyyMM", requiredMode = Schema.RequiredMode.REQUIRED, example = "202602")
    @NotEmpty(message = "结账期间不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "结账期间格式非法，应为 yyyyMM")
    private String closingPeriod;

    @Schema(description = "结账类型（默认 MONTHLY）", example = "MONTHLY")
    @InEnum(FinanceClosingTypeEnum.class)
    private String closingType;

    @Schema(description = "备注")
    private String remark;
}
