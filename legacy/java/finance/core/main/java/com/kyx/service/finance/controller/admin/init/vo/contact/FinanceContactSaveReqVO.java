package com.kyx.service.finance.controller.admin.init.vo.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 往来信息保存请求 VO
 *
 * @author xyang
 */
@Data
@Schema(description = "往来信息保存请求（租户级共享主数据）")
public class FinanceContactSaveReqVO {

    @Schema(description = "往来ID（更新时必填）")
    private Long id;

    @Schema(description = "分组ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分组不能为空")
    private Long groupId;

    @Schema(description = "往来名称（租户内共享）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "往来名称不能为空")
    private String contactName;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "账户类型（支付宝/银行卡等）")
    private String accountType;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "账号")
    private String accountNo;

    @Schema(description = "姓名")
    private String ownerName;

    @Schema(description = "联系方式")
    private String phone;

    @Schema(description = "状态：0 启用，1 停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
