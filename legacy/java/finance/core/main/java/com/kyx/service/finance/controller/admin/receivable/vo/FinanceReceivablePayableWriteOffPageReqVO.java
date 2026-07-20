package com.kyx.service.finance.controller.admin.receivable.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - 往来账核销分页 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 往来账核销分页 Request VO")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FinanceReceivablePayableWriteOffPageReqVO extends PageParam {

    @Schema(description = "账套", example = "1")
    private Long companyId;

    @Schema(description = "往来账编号", example = "1")
    private Long arpId;

    @Schema(description = "Transaction ID", example = "1001")
    private Long transactionId;

    @Schema(description = "核销日期范围", example = "[2026-02-01 00:00:00, 2026-02-28 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] writeOffDate;
}
