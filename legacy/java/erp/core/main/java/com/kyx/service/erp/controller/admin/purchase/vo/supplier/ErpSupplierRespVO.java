package com.kyx.service.erp.controller.admin.purchase.vo.supplier;

import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import com.kyx.service.business.enums.DictTypeConstants;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 供应商 Response VO")
@Data
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class ErpSupplierRespVO {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17791")
    @ExcelProperty("供应商编号")
    private Long id;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "MK")
    @ExcelProperty("供应商名称")
    private String name;

    @Schema(description = "联系人", example = "MK")
    @ExcelProperty("联系人")
    private String contact;

    @Schema(description = "手机号码", example = "15601691300")
    @ExcelProperty("手机号码")
    private String mobile;

    @Schema(description = "联系电话", example = "18818288888")
    @ExcelProperty("联系电话")
    private String telephone;

    @Schema(description = "电子邮箱", example = "76853@qq.com")
    @ExcelProperty("电子邮箱")
    private String email;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "开启状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "纳税人识别号", example = "91130803MA098BY05W")
    @ExcelProperty("纳税人识别号")
    private String taxNo;

    @Schema(description = "税率", example = "10")
    @ExcelProperty("税率")
    private BigDecimal taxPercent;

    @Schema(description = "付款方式配置（JSON格式）", example = "[{\"id\":\"1\",\"type\":\"bank_transfer\",\"name\":\"银行转账\"}]")
    private String paymentMethods;

    @Schema(description = "常用收货地点配置（JSON格式）", example = "[{\"id\":\"1\",\"name\":\"默认收货地址\",\"contact\":\"张三\"}]")
    private String deliveryAddresses;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}