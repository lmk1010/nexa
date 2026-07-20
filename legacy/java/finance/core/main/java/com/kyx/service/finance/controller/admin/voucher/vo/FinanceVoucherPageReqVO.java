package com.kyx.service.finance.controller.admin.voucher.vo;

import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.finance.enums.FinanceVoucherStatusEnum;
import com.kyx.service.finance.enums.FinanceVoucherTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - 凭证分页 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 凭证分页 Request VO")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FinanceVoucherPageReqVO extends PageParam {

    @Schema(description = "账套ID")
    private Long companyId;

    @Schema(description = "凭证号", example = "VCH202602270001")
    private String voucherNo;

    @Schema(description = "凭证类型", example = "GENERAL")
    @InEnum(FinanceVoucherTypeEnum.class)
    private String voucherType;

    @Schema(description = "凭证状态", example = "DRAFT")
    @InEnum(FinanceVoucherStatusEnum.class)
    private String status;

    @Schema(description = "凭证日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] voucherDate;
}
