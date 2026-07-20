package com.kyx.service.finance.controller.admin.transaction.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.finance.enums.FinanceTransactionStatusEnum;
import com.kyx.service.finance.enums.FinanceTransactionTypeEnum;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 资金流水新增 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 资金流水新增 Request VO")
public class FinanceTransactionSaveReqVO {

    @Schema(description = "账套ID。创建时必传，更新时可不传（沿用原值）")
    @DiffLogField(name = "账套ID")
    private Long companyId;

    @Schema(description = "交易单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "TRX202602090001")
    @NotEmpty(message = "交易单号不能为空")
    @DiffLogField(name = "交易单号")
    private String transactionNo;

    @Schema(description = "交易日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "交易日期不能为空")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @DiffLogField(name = "交易日期")
    private LocalDateTime transactionDate;

    @Schema(description = "交易金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "交易金额不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "交易金额必须大于0")
    @DiffLogField(name = "交易金额")
    private BigDecimal amount;

    @Schema(description = "交易类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "INCOME")
    @NotNull(message = "交易类型不能为空")
    @InEnum(FinanceTransactionTypeEnum.class)
    @DiffLogField(name = "交易类型")
    private String transactionType;

    @Schema(description = "账户ID（租户级共享主数据）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账户ID不能为空")
    @DiffLogField(name = "账户ID")
    private Long accountId;

    @Schema(description = "对方账户ID（转账场景，租户级共享主数据）", example = "2")
    @DiffLogField(name = "对方账户ID")
    private Long oppositeAccountId;

    @Schema(description = "科目编码")
    @DiffLogField(name = "科目编码")
    private String subjectCode;

    @Schema(description = "往来对象ID（租户级共享主数据）")
    @DiffLogField(name = "往来对象ID")
    private Long contactId;

    @Schema(description = "业务引用类型（兼容字段名：category）")
    @DiffLogField(name = "交易分类")
    private String category;

    @Schema(description = "业务引用单号")
    @DiffLogField(name = "业务引用单号")
    private String businessRefNo;

    @Schema(description = "摘要（兼容字段名：description）")
    @DiffLogField(name = "描述")
    private String description;

    @Schema(description = "状态：DRAFT/SUCCESS/INVALID", example = "SUCCESS")
    @NotNull(message = "状态不能为空")
    @InEnum(FinanceTransactionStatusEnum.class)
    @DiffLogField(name = "状态")
    private String status;

    @Schema(description = "业务引用ID（兼容字段名：relatedBusinessId）")
    @DiffLogField(name = "关联业务ID")
    private Long relatedBusinessId;

    @Schema(description = "业务模式：EXTERNAL/INTERNAL（兼容字段名：businessType）")
    @DiffLogField(name = "业务模式")
    private String businessType;

    @Schema(description = "税额", example = "0.00")
    @DiffLogField(name = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "标签JSON")
    @DiffLogField(name = "标签JSON")
    private String tagJson;

    @Schema(description = "附件JSON")
    @DiffLogField(name = "附件JSON")
    private String attachmentJson;

    @Schema(description = "关联凭证ID")
    @DiffLogField(name = "关联凭证ID")
    private Long voucherId;
}
