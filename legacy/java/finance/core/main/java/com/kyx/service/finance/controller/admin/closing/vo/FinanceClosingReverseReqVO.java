package com.kyx.service.finance.controller.admin.closing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理后台 - 反结账 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 反结账 Request VO")
public class FinanceClosingReverseReqVO {

    @Schema(description = "结账记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "结账记录ID不能为空")
    private Long id;

    @Schema(description = "备注")
    private String remark;
}
