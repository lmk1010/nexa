package com.kyx.service.finance.controller.admin.init.vo.opening;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 期初余额列表查询请求 VO（不分页）
 */
@Data
@Schema(description = "期初余额列表查询请求")
public class FinanceOpeningBalanceListReqVO {

    @Schema(description = "账套ID")
    private Long companyId;

    @Schema(description = "期间，格式yyyyMM")
    private String period;

    @Schema(description = "科目编码")
    private String subjectCode;

    @Schema(description = "科目名称")
    private String subjectName;

    @Schema(description = "状态：0启用 1停用")
    private Integer status;

    @Schema(description = "是否锁定：false未锁定 true已锁定")
    private Boolean locked;

}
