package com.kyx.service.finance.controller.admin.voucher.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 凭证 Response VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 凭证 Response VO")
@ExcelIgnoreUnannotated
public class FinanceVoucherRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("ID")
    private Long id;

    @Schema(description = "账套ID")
    @ExcelProperty("账套ID")
    private Long companyId;

    @Schema(description = "凭证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "VCH202602270001")
    @ExcelProperty("凭证号")
    private String voucherNo;

    @Schema(description = "凭证日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("凭证日期")
    private LocalDateTime voucherDate;

    @Schema(description = "凭证期间 yyyyMM", requiredMode = Schema.RequiredMode.REQUIRED, example = "202602")
    @ExcelProperty("凭证期间")
    private String voucherPeriod;

    @Schema(description = "凭证类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "GENERAL")
    @ExcelProperty("凭证类型")
    private String voucherType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "DRAFT")
    @ExcelProperty("状态")
    private String status;

    @Schema(description = "借方合计", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @ExcelProperty("借方合计")
    private BigDecimal totalDebit;

    @Schema(description = "贷方合计", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @ExcelProperty("贷方合计")
    private BigDecimal totalCredit;

    @Schema(description = "来源类型")
    @ExcelProperty("来源类型")
    private String sourceType;

    @Schema(description = "来源单号")
    @ExcelProperty("来源单号")
    private String sourceNo;

    @Schema(description = "来源ID")
    @ExcelProperty("来源ID")
    private Long sourceId;

    @Schema(description = "摘要")
    @ExcelProperty("摘要")
    private String description;

    @Schema(description = "附件JSON")
    @ExcelProperty("附件JSON")
    private String attachmentJson;

    @Schema(description = "审核人")
    @ExcelProperty("审核人")
    private String approvedBy;

    @Schema(description = "审核时间")
    @ExcelProperty("审核时间")
    private LocalDateTime approvedTime;

    @Schema(description = "过账人")
    @ExcelProperty("过账人")
    private String postedBy;

    @Schema(description = "过账时间")
    @ExcelProperty("过账时间")
    private LocalDateTime postedTime;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "凭证明细")
    private List<FinanceVoucherDetailRespVO> details;
}
