package com.kyx.service.finance.controller.admin.voucher.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 凭证更新 Request VO
 *
 * @author xyang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台 - 凭证更新 Request VO")
public class FinanceVoucherUpdateReqVO extends FinanceVoucherSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "主键ID不能为空")
    private Long id;
}
