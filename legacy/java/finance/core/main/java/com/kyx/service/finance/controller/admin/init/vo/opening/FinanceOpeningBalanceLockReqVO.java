package com.kyx.service.finance.controller.admin.init.vo.opening;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 期初余额锁定请求 VO
 */
@Data
@Schema(description = "期初余额锁定请求")
public class FinanceOpeningBalanceLockReqVO {

    @Schema(description = "账套ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套ID不能为空")
    private Long companyId;

    @Schema(description = "期间，格式yyyyMM", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "期间不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "期间格式必须为yyyyMM")
    private String period;

    @Schema(description = "是否锁定：false解锁 true锁定", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "锁定状态不能为空")
    private Boolean locked;

}
