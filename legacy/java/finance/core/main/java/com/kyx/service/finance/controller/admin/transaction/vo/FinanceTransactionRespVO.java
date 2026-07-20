package com.kyx.service.finance.controller.admin.transaction.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水 Response VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 资金流水 Response VO")
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class FinanceTransactionRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "交易单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRX202602090001")
    @ExcelProperty("交易单号")
    private String transactionNo;

    @Schema(description = "交易日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("交易日期")
    private LocalDateTime transactionDate;

    @Schema(description = "交易金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @ExcelProperty("交易金额")
    private BigDecimal amount;

    @Schema(description = "交易类型：INCOME收入/EXPENSE支出/TRANSFER转账")
    @ExcelProperty("交易类型")
    private String transactionType;

    @Schema(description = "账户ID")
    @ExcelProperty("账户ID")
    private Long accountId;

    @Schema(description = "账户名称")
    @ExcelProperty("账户名称")
    private String accountName;

    @Schema(description = "账号")
    @ExcelProperty("账号")
    private String accountNumber;

    @Schema(description = "账户类型")
    @ExcelProperty("账户类型")
    private String accountType;

    @Schema(description = "对方账户ID（转账场景）")
    @ExcelProperty("对方账户ID")
    private Long oppositeAccountId;

    @Schema(description = "对方账户名称")
    @ExcelProperty("对方账户名称")
    private String oppositeAccountName;

    @Schema(description = "对方账号")
    @ExcelProperty("对方账号")
    private String oppositeAccountNumber;

    @Schema(description = "对方账户类型")
    @ExcelProperty("对方账户类型")
    private String oppositeAccountType;

    @Schema(description = "交易分类")
    @ExcelProperty("交易分类")
    private String category;

    @Schema(description = "描述")
    @ExcelProperty("描述")
    private String description;

    @Schema(description = "状态：PENDING处理中/SUCCESS成功/FAILED失败/REVERSED已作废")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "关联业务ID")
    @ExcelProperty("关联业务ID")
    private Long relatedBusinessId;

    @Schema(description = "业务类型：RECEIPT收款/PAYMENT付款/TRANSFER转账/OTHER其他")
    @ExcelProperty("业务类型")
    private String businessType;

    @Schema(description = "税额")
    @ExcelProperty("税额")
    private BigDecimal taxAmount;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
