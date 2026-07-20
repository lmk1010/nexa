package com.kyx.service.erp.controller.admin.purchase.vo.supplier;

import com.kyx.foundation.common.enums.CommonStatusEnum;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.foundation.common.validation.Mobile;
import com.kyx.foundation.common.validation.Telephone;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 供应商新增/修改 Request VO")
@Data
public class ErpSupplierSaveReqVO {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17791")
    private Long id;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "MK")
    @NotEmpty(message = "供应商名称不能为空")
    private String name;

    @Schema(description = "联系人", example = "MK")
    private String contact;

    @Schema(description = "手机号码", example = "15601691300")
    @Mobile
    private String mobile;

    @Schema(description = "联系电话", example = "18818288888")
    @Telephone
    private String telephone;

    @Schema(description = "电子邮箱", example = "76853@qq.com")
    @Email
    private String email;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "开启状态不能为空")
    @InEnum(value = CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "纳税人识别号", example = "91130803MA098BY05W")
    private String taxNo;

    @Schema(description = "税率", example = "10")
    private BigDecimal taxPercent;

    @Schema(description = "付款方式配置（JSON格式）", example = "[{\"id\":\"1\",\"type\":\"bank_transfer\",\"name\":\"银行转账\"}]")
    private String paymentMethods;

    @Schema(description = "常用收货地点配置（JSON格式）", example = "[{\"id\":\"1\",\"name\":\"默认收货地址\",\"contact\":\"张三\"}]")
    private String deliveryAddresses;

}