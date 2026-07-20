package com.kyx.service.finance.controller.admin.init.vo.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 往来批量删除请求 VO
 */
@Data
@Schema(description = "往来批量删除请求")
public class FinanceContactBatchDeleteReqVO {

    @Schema(description = "往来ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "往来ID列表不能为空")
    private List<Long> ids;
}
