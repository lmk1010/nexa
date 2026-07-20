package com.kyx.service.finance.controller.admin.receivable.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 往来账 Response VO
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 往来账 Response VO")
@ExcelIgnoreUnannotated
public class FinanceReceivablePayableRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "账套ID")
    @ExcelProperty("账套ID")
    private Long companyId;

    @Schema(description = "往来单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("往来单位")
    private Long contactId;

    @Schema(description = "单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ARP202602110001")
    @ExcelProperty("单号")
    private String billNo;

    @Schema(description = "单据日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("单据日期")
    private LocalDateTime billDate;

    @Schema(description = "单据金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @ExcelProperty("单据金额")
    private BigDecimal amount;

    @Schema(description = "已核销金额")
    @ExcelProperty("已核销金额")
    private BigDecimal paidAmount;

    @Schema(description = "未核销余额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @ExcelProperty("未核销余额")
    private BigDecimal balance;

    @Schema(description = "类型")
    @ExcelProperty("类型")
    private String type;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "到期日期")
    @ExcelProperty("到期日期")
    private LocalDateTime dueDate;

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
