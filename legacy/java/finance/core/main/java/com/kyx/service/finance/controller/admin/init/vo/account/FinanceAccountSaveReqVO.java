package com.kyx.service.finance.controller.admin.init.vo.account;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 账户保存请求 VO
 */
@Data
@Schema(description = "账户保存请求（租户级共享主数据）")
public class FinanceAccountSaveReqVO {

    @Schema(description = "账户ID，更新时必填")
    private Long id;

    @Schema(description = "账户别名")
    private String accountAlias;

    @Schema(description = "账户类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账户类型不能为空")
    private String accountType;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账号不能为空")
    private String accountNumber;

    @Schema(description = "税号")
    private String taxNo;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "启停状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "启停状态不能为空")
    @InEnum(value = CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "银行名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "银行名称不能为空")
    private String bankName;

    @Schema(description = "省编码")
    private String provinceCode;

    @Schema(description = "市编码")
    private String cityCode;

    @Schema(description = "区编码")
    private String districtCode;

    @Schema(description = "支行名称")
    private String branchName;

    @Schema(description = "企业主体")
    private String companyEntity;

    @Schema(description = "账户标签文本（逗号分隔）")
    private String accountTagText;

    @Schema(description = "收款是否需要手续费")
    private Boolean receiptFeeEnabled;

    @Schema(description = "付款是否需要手续费")
    private Boolean paymentFeeEnabled;

    @Schema(description = "备注")
    private String remark;
}
