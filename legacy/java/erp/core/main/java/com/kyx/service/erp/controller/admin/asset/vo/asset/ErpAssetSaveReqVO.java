package com.kyx.service.erp.controller.admin.asset.vo.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 资产新增/修改 Request VO")
@Data
public class ErpAssetSaveReqVO {

    @Schema(description = "资产编号", example = "1")
    private Long id;

    @Schema(description = "资产编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET001")
    @NotEmpty(message = "资产编码不能为空")
    private String assetNo;

    @Schema(description = "资产名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "联想电脑")
    @NotEmpty(message = "资产名称不能为空")
    private String name;

    @Schema(description = "资产类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公设备")
    @NotEmpty(message = "资产类型不能为空")
    private String type;

    @Schema(description = "资产分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "规格型号", example = "ThinkPad T14")
    private String specification;

    @Schema(description = "品牌", example = "联想")
    private String brand;

    @Schema(description = "型号", example = "T14")
    private String model;

    @Schema(description = "序列号", example = "SN123456789")
    private String serialNumber;

    @Schema(description = "购置日期", example = "2024-01-01")
    private LocalDate purchaseDate;

    @Schema(description = "购置价格，单位：元", example = "8000.00")
    private BigDecimal purchasePrice;

    @Schema(description = "当前价值，单位：元", example = "6000.00")
    private BigDecimal currentValue;

    @Schema(description = "折旧率，百分比", example = "20.00")
    private BigDecimal depreciationRate;

    @Schema(description = "使用年限（年）", example = "5")
    private Integer usefulLife;

    @Schema(description = "保修到期日期", example = "2027-01-01")
    private LocalDate warrantyDate;

    @Schema(description = "存放位置", example = "办公室A区")
    private String location;

    @Schema(description = "管理部门", example = "1")
    private Long deptId;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "资产状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产状态不能为空")
    private Integer status;

    @Schema(description = "资产状况", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产状况不能为空")
    private Integer conditionStatus;

    @Schema(description = "备注", example = "办公使用")
    private String remark;

    @Schema(description = "附件URL", example = "http://example.com/file.pdf")
    private String attachmentUrl;

    @Schema(description = "附件文件ID列表（支持数字ID和UUID字符串）", example = "[\"abc123def456\", \"xyz789abc123\"]")
    private List<String> fileIds;

} 