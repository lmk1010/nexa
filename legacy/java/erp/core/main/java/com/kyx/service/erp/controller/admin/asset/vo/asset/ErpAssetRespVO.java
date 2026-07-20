package com.kyx.service.erp.controller.admin.asset.vo.asset;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.kyx.service.erp.controller.admin.asset.vo.attachment.ErpAssetAttachmentRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 资产 Response VO")
@Data
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class ErpAssetRespVO {

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("资产编号")
    private Long id;

    @Schema(description = "资产编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET001")
    @ExcelProperty("资产编码")
    private String assetNo;

    @Schema(description = "资产名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "联想电脑")
    @ExcelProperty("资产名称")
    private String name;

    @Schema(description = "资产类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公设备")
    @ExcelProperty("资产类型")
    private String type;

    @Schema(description = "资产分类编号", example = "1")
    private Long categoryId;
    @Schema(description = "资产分类名称", example = "电脑设备")
    @ExcelProperty("资产分类")
    private String categoryName;

    @Schema(description = "规格型号", example = "ThinkPad T14")
    @ExcelProperty("规格型号")
    private String specification;

    @Schema(description = "品牌", example = "联想")
    @ExcelProperty("品牌")
    private String brand;

    @Schema(description = "型号", example = "T14")
    @ExcelProperty("型号")
    private String model;

    @Schema(description = "序列号", example = "SN123456789")
    @ExcelProperty("序列号")
    private String serialNumber;

    @Schema(description = "购置日期", example = "2024-01-01")
    @ExcelProperty("购置日期")
    private LocalDate purchaseDate;

    @Schema(description = "购置价格，单位：元", example = "8000.00")
    @ExcelProperty("购置价格，单位：元")
    private BigDecimal purchasePrice;

    @Schema(description = "当前价值，单位：元", example = "6000.00")
    @ExcelProperty("当前价值，单位：元")
    private BigDecimal currentValue;

    @Schema(description = "折旧率，百分比", example = "20.00")
    @ExcelProperty("折旧率，百分比")
    private BigDecimal depreciationRate;

    @Schema(description = "使用年限（年）", example = "5")
    @ExcelProperty("使用年限（年）")
    private Integer usefulLife;

    @Schema(description = "保修到期日期", example = "2027-01-01")
    @ExcelProperty("保修到期日期")
    private LocalDate warrantyDate;

    @Schema(description = "存放位置", example = "办公室A区")
    @ExcelProperty("存放位置")
    private String location;

    @Schema(description = "管理部门编号", example = "1")
    private Long deptId;
    @Schema(description = "管理部门名称", example = "技术部")
    @ExcelProperty("管理部门")
    private String deptName;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;
    @Schema(description = "供应商名称", example = "联想中国")
    @ExcelProperty("供应商")
    private String supplierName;

    @Schema(description = "资产状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("资产状态")
    private Integer status;

    @Schema(description = "资产状况", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("资产状况")
    private Integer conditionStatus;

    @Schema(description = "备注", example = "办公使用")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "附件URL", example = "http://example.com/file.pdf")
    private String attachmentUrl;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "附件列表")
    private List<ErpAssetAttachmentRespVO> attachments;

} 