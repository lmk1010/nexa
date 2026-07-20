package com.kyx.service.finance.controller.admin.init.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 账户选项响应 VO
 */
@Data
@Schema(description = "账户选项")
public class FinanceAccountOptionRespVO {

    @Schema(description = "选项ID")
    private Long id;

    @Schema(description = "选项值")
    private String optionValue;
}

