package com.kyx.service.finance.controller.admin.receivable.vo;

import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.finance.enums.FinanceReceivablePayableStatusEnum;
import com.kyx.service.finance.enums.FinanceReceivablePayableTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - 往来账分页 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 往来账分页 Request VO")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FinanceReceivablePayablePageReqVO extends PageParam {

    @Schema(description = "账套ID")
    private Long companyId;

    @Schema(description = "单号", example = "ARP202602110001")
    private String billNo;

    @Schema(description = "往来单位ID", example = "1")
    private Long contactId;

    @Schema(description = "类型", example = "RECEIVABLE")
    @InEnum(FinanceReceivablePayableTypeEnum.class)
    private String type;

    @Schema(description = "状态", example = "UNPAID")
    @InEnum(FinanceReceivablePayableStatusEnum.class)
    private String status;

    @Schema(description = "单据日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] billDate;
}
