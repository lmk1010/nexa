package com.kyx.service.finance.controller.admin.init.vo.opening;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 期初余额滚动请求 VO
 * @author xyang
 */
@Data
@Schema(description = "期初余额滚动请求")
public class FinanceOpeningBalanceRollReqVO {

    @Schema(description = "账套", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套不能为空")
    private Long companyId;

    @Schema(description = "来源期间，格式yyyyMM", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "来源期间不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "来源期间格式必须为yyyyMM")
    private String fromPeriod;

    @Schema(description = "目标期间，格式yyyyMM，不传默认来源期间下月")
    @Pattern(regexp = "^\\d{6}$", message = "目标期间格式必须为yyyyMM")
    private String toPeriod;

    @Schema(description = "目标期间已有科目是否覆盖，默认false")
    private Boolean overwrite;
}

