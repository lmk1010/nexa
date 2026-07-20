package com.kyx.service.finance.controller.admin.closing.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - 月末结账 Response VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 月末结账 Response VO")
@ExcelIgnoreUnannotated
public class FinanceClosingRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "账套ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("账套ID")
    private Long companyId;

    @Schema(description = "结账期间 yyyyMM", requiredMode = Schema.RequiredMode.REQUIRED, example = "202602")
    @ExcelProperty("结账期间")
    private String closingPeriod;

    @Schema(description = "结账类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "MONTHLY")
    @ExcelProperty("结账类型")
    private String closingType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUCCESS")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "结账检查结果")
    @ExcelProperty("结账检查结果")
    private String precheckResult;

    @Schema(description = "损益结转凭证ID")
    @ExcelProperty("损益结转凭证ID")
    private Long profitTransferVoucherId;

    @Schema(description = "损益结转凭证号")
    @ExcelProperty("损益结转凭证号")
    private String profitTransferVoucherNo;

    @Schema(description = "损益结转凭证状态")
    @ExcelProperty("损益结转凭证状态")
    private String profitTransferVoucherStatus;

    @Schema(description = "结账时间")
    @ExcelProperty("结账时间")
    private LocalDateTime closeTime;

    @Schema(description = "结账人")
    @ExcelProperty("结账人")
    private String closedBy;

    @Schema(description = "反结账时间")
    @ExcelProperty("反结账时间")
    private LocalDateTime reversedTime;

    @Schema(description = "反结账人")
    @ExcelProperty("反结账人")
    private String reversedBy;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
