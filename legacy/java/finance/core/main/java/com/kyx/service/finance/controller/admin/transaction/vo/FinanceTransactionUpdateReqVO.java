package com.kyx.service.finance.controller.admin.transaction.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 资金流水更新 Request VO
 *
 * @author xyang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台 - 资金流水更新 Request VO")
public class FinanceTransactionUpdateReqVO extends FinanceTransactionSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "流水ID不能为空")
    private Long id;
}
