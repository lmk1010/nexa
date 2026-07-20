package com.kyx.service.finance.controller.admin.init.vo.account;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 账户批量状态更新请求 VO
 */
@Data
@Schema(description = "账户批量状态更新请求")
public class FinanceAccountBatchStatusUpdateReqVO {

    @Schema(description = "账户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "账户ID列表不能为空")
    private List<Long> ids;

    @Schema(description = "状态：0 启用，1 停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class)
    private Integer status;
}

