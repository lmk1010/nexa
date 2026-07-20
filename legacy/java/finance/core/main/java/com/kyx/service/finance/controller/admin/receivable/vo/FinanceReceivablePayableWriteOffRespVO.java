package com.kyx.service.finance.controller.admin.receivable.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 往来账核销 Response VO
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 往来账核销 Response VO")
@ExcelIgnoreUnannotated
public class FinanceReceivablePayableWriteOffRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "账套ID")
    @ExcelProperty("账套ID")
    private Long companyId;

    @Schema(description = "往来账ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("往来账ID")
    private Long arpId;

    @Schema(description = "核销号")
    @ExcelProperty("核销号")
    private String writeOffNo;

    @Schema(description = "关联流水ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @ExcelProperty("关联流水ID")
    private Long transactionId;

    @Schema(description = "核销金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @ExcelProperty("核销金额")
    private BigDecimal amount;

    @Schema(description = "核销日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("核销日期")
    private LocalDateTime writeOffDate;

    @Schema(description = "摘要（兼容字段名：description）")
    @ExcelProperty("摘要")
    private String description;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
