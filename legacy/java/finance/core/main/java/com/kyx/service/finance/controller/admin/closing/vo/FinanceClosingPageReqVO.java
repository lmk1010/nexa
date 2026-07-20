package com.kyx.service.finance.controller.admin.closing.vo;

import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.finance.enums.FinanceClosingStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 月末结账分页 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 月末结账分页 Request VO")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FinanceClosingPageReqVO extends PageParam {

    @Schema(description = "账套ID")
    private Long companyId;

    @Schema(description = "结账期间 yyyyMM", example = "202602")
    private String closingPeriod;

    @Schema(description = "状态", example = "SUCCESS")
    @InEnum(FinanceClosingStatusEnum.class)
    private String status;
}
