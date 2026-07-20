package com.kyx.service.finance.controller.admin.init.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 账户选项编辑请求 VO
 */
@Data
@Schema(description = "账户选项编辑请求")
public class FinanceAccountOptionUpdateReqVO {

    @Schema(description = "选项ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "选项ID不能为空")
    private Long id;

    @Schema(description = "选项值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "选项值不能为空")
    private String optionValue;
}

