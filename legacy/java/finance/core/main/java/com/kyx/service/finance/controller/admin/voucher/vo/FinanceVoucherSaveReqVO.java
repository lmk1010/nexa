package com.kyx.service.finance.controller.admin.voucher.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.finance.enums.FinanceVoucherStatusEnum;
import com.kyx.service.finance.enums.FinanceVoucherTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 管理后台 - 凭证新增 Request VO
 *
 * @author xyang
 */
@Data
@Schema(description = "管理后台 - 凭证新增 Request VO")
public class FinanceVoucherSaveReqVO {

    @Schema(description = "账套ID。创建时必传，更新时可不传（沿用原值）")
    private Long companyId;

    @Schema(description = "凭证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "VCH202602270001")
    @NotEmpty(message = "凭证号不能为空")
    private String voucherNo;

    @Schema(description = "凭证日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "凭证日期不能为空")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime voucherDate;

    @Schema(description = "凭证类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "GENERAL")
    @NotNull(message = "凭证类型不能为空")
    @InEnum(FinanceVoucherTypeEnum.class)
    private String voucherType;

    @Schema(description = "状态（默认 DRAFT）", example = "DRAFT")
    @InEnum(FinanceVoucherStatusEnum.class)
    private String status;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "来源ID")
    private Long sourceId;

    @Schema(description = "摘要（兼容字段名：description）")
    private String description;

    @Schema(description = "附件JSON")
    private String attachmentJson;

    @Schema(description = "凭证明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "凭证明细不能为空")
    @Valid
    private List<FinanceVoucherDetailSaveReqVO> details;
}
