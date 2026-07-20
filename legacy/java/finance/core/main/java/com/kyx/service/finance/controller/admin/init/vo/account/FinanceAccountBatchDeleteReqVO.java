package com.kyx.service.finance.controller.admin.init.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 账户批量删除请求 VO
 */
@Data
@Schema(description = "账户批量删除请求")
public class FinanceAccountBatchDeleteReqVO {

    @Schema(description = "账户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "账户ID列表不能为空")
    private List<Long> ids;
}

