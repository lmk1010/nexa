package com.kyx.service.finance.controller.admin.receivable.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.finance.enums.FinanceReceivablePayableStatusEnum;
import com.kyx.service.finance.enums.FinanceReceivablePayableTypeEnum;
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
 * 管理后台 - 往来账新增 Request VO
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 往来账新增 Request VO")
public class FinanceReceivablePayableSaveReqVO {

    @Schema(description = "账套ID。创建时必传，更新时可不传（沿用原值）")
    private Long companyId;

    @Schema(description = "往来单位ID（租户级共享主数据）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "往来单位不能为空")
    private Long contactId;

    @Schema(description = "单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ARP202602110001")
    @NotEmpty(message = "单号不能为空")
    private String billNo;

    @Schema(description = "单据日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "单据日期不能为空")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime billDate;

    @Schema(description = "单据金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    @NotNull(message = "单据金额不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "单据金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "未核销余额", example = "1000.00")
    @DecimalMin(value = "0.00", inclusive = true, message = "余额不能小于0")
    private BigDecimal balance;

    @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "RECEIVABLE")
    @NotNull(message = "类型不能为空")
    @InEnum(FinanceReceivablePayableTypeEnum.class)
    private String type;

    @Schema(description = "状态", example = "UNPAID")
    @InEnum(FinanceReceivablePayableStatusEnum.class)
    private String status;

    @Schema(description = "到期日期")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime dueDate;

    @Schema(description = "摘要（兼容字段名：description）")
    private String description;

    @Schema(description = "备注")
    private String remark;
}
