package com.kyx.service.finance.controller.admin.init.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 账户选项新增请求 VO
 */
@Data
@Schema(description = "账户选项新增请求")
public class FinanceAccountOptionCreateReqVO {

    @Schema(description = "选项值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "选项值不能为空")
    private String optionValue;
}

