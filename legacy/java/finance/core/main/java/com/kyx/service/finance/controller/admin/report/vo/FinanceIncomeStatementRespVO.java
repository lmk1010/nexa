package com.kyx.service.finance.controller.admin.report.vo;

import com.kyx.foundation.common.biz.tree.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 管理后台 - 利润表行数据 Response VO
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理后台 - 利润表行数据 Response VO")
public class FinanceIncomeStatementRespVO extends TreeNode<FinanceIncomeStatementRespVO> {

    @Schema(description = "项目编码", example = "6001")
    private String itemCode;

    @Schema(description = "项目名称", example = "主营业务收入")
    private String itemName;

    @Schema(description = "本期收入", example = "12345.67")
    private BigDecimal currentIncome;

    @Schema(description = "本期支出", example = "76543.21")
    private BigDecimal currentExpense;
}
