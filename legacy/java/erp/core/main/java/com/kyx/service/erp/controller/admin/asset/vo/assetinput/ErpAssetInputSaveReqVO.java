package com.kyx.service.erp.controller.admin.asset.vo.assetinput;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - ERP 资产录入申请新增/修改 Request VO")
@Data
public class ErpAssetInputSaveReqVO {

    @Schema(description = "申请编号", example = "1")
    private Long id;

    @Schema(description = "资产编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A202501001")
    @NotBlank(message = "资产编码不能为空")
    private String assetNo;

    @Schema(description = "资产名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "笔记本电脑")
    @NotBlank(message = "资产名称不能为空")
    private String name;

    @Schema(description = "资产类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "IT设备")
    @NotBlank(message = "资产类型不能为空")
    private String type;

    @Schema(description = "资产分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "规格型号", example = "ThinkPad X1")
    private String specification;

    @Schema(description = "品牌", example = "联想")
    private String brand;

    @Schema(description = "型号", example = "X1 Carbon")
    private String model;

    @Schema(description = "序列号", example = "SN123456789")
    private String serialNumber;

    @Schema(description = "购置日期", example = "2025-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate purchaseDate;

    @Schema(description = "购置价格，单位：元", example = "8999.00")
    private BigDecimal purchasePrice;

    @Schema(description = "当前价值，单位：元", example = "7999.00")
    private BigDecimal currentValue;

    @Schema(description = "折旧率，百分比", example = "10.00")
    private BigDecimal depreciationRate;

    @Schema(description = "使用年限（年）", example = "5")
    private Integer usefulLife;

    @Schema(description = "保修到期日期", example = "2028-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate warrantyDate;

    @Schema(description = "存放位置", example = "办公室A座301")
    private String location;

    @Schema(description = "管理部门", example = "1")
    private Long deptId;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "资产状况", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产状况不能为空")
    private Integer conditionStatus;

    @Schema(description = "备注", example = "申请录入IT设备")
    private String remark;

    @Schema(description = "附件文件ID数组", example = "[1, 2, 3]")
    private List<Long> fileIds;

} 