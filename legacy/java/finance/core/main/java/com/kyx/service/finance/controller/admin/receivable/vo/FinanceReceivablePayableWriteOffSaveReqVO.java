package com.kyx.service.finance.controller.admin.receivable.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - 往来账核销新增 Request VO
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 往来账核销新增 Request VO")
public class FinanceReceivablePayableWriteOffSaveReqVO {

    @Schema(description = "往来账ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "往来账ID不能为空")
    @DiffLogField(name = "往来账ID")
    private Long arpId;

    @Schema(description = "核销金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "核销金额不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "核销金额必须大于0")
    @DiffLogField(name = "核销金额")
    private BigDecimal amount;

    @Schema(description = "核销日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "核销日期不能为空")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @DiffLogField(name = "核销日期")
    private LocalDateTime writeOffDate;

    @Schema(description = "摘要（兼容字段名：description）")
    @DiffLogField(name = "摘要")
    private String description;

    @Schema(description = "备注")
    @DiffLogField(name = "备注")
    private String remark;

    @Schema(description = "关联流水ID", example = "1001")
    @DiffLogField(name = "关联流水ID")
    private Long transactionId;

    @Schema(description = "自动生成流水时使用的账户ID", example = "2")
    @DiffLogField(name = "自动流水账户ID")
    private Long accountId;

    @Schema(description = "自动生成流水时的单号，不传则系统自动生成", example = "WO202602110001")
    @DiffLogField(name = "自动流水单号")
    private String transactionNo;

    @Schema(description = "自动生成流水时的科目编码", example = "6001")
    @DiffLogField(name = "自动流水科目")
    private String subjectCode;

    @Schema(description = "自动生成流水时的业务引用类型", example = "RECEIPT")
    @DiffLogField(name = "自动流水业务引用类型")
    private String category;
}
