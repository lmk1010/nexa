package com.kyx.service.finance.controller.admin.init.vo.contact;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 往来状态更新请求 VO
 */
@Data
@Schema(description = "往来状态更新请求")
public class FinanceContactStatusUpdateReqVO {

    @Schema(description = "往来ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "往来ID不能为空")
    private Long id;

    @Schema(description = "状态：0 启用，1 停用", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    @InEnum(value = CommonStatusEnum.class)
    private Integer status;
}
