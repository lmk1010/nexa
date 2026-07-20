package com.kyx.service.finance.controller.admin.transaction.vo;

import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.finance.enums.FinanceTransactionTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 资金流水分页 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 资金流水分页 Request VO")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FinanceTransactionPageReqVO extends PageParam {

    @Schema(description = "账套ID")
    private Long companyId;

    @Schema(description = "交易单号", example = "TRX202602090001")
    private String transactionNo;

    @Schema(description = "交易类型", example = "INCOME")
    @InEnum(FinanceTransactionTypeEnum.class)
    private String transactionType;

    @Schema(description = "状态：DRAFT/SUCCESS/INVALID", example = "SUCCESS")
    private String status;

    @Schema(description = "账户ID", example = "1")
    private Long accountId;

    @Schema(description = "对方账户ID", example = "2")
    private Long oppositeAccountId;

    @Schema(description = "科目编码", example = "1001")
    private String subjectCode;

    @Schema(description = "往来对象ID", example = "1")
    private Long contactId;

    @Schema(description = "业务引用类型（兼容字段名：category）")
    private String category;

    @Schema(description = "业务引用单号")
    private String businessRefNo;

    @Schema(description = "业务模式（兼容字段名：businessType）")
    private String businessType;

    @Schema(description = "交易日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] transactionDate;
}
