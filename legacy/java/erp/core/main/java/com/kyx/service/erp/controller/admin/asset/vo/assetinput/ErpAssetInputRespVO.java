package com.kyx.service.erp.controller.admin.asset.vo.assetinput;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 资产录入申请 Response VO")
@Data
public class ErpAssetInputRespVO {

    @Schema(description = "申请编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "录入申请编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "AI202501001")
    private String inputNo;

    @Schema(description = "资产编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A202501001")
    private String assetNo;

    @Schema(description = "资产名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "笔记本电脑")
    private String name;

    @Schema(description = "资产类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "IT设备")
    private String type;

    @Schema(description = "资产分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "资产分类名称", example = "办公设备")
    private String categoryName;

    @Schema(description = "规格型号", example = "ThinkPad X1")
    private String specification;

    @Schema(description = "品牌", example = "联想")
    private String brand;

    @Schema(description = "型号", example = "X1 Carbon")
    private String model;

    @Schema(description = "序列号", example = "SN123456789")
    private String serialNumber;

    @Schema(description = "购置日期", example = "2025-01-01")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY, timezone = "GMT+8")
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
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY, timezone = "GMT+8")
    private LocalDate warrantyDate;

    @Schema(description = "存放位置", example = "办公室A座301")
    private String location;

    @Schema(description = "管理部门", example = "1")
    private Long deptId;

    @Schema(description = "管理部门名称", example = "技术部")
    private String deptName;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "供应商名称", example = "联想集团")
    private String supplierName;

    @Schema(description = "申请状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "申请状态名称", example = "待审批")
    private String statusName;

    @Schema(description = "资产状况", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer conditionStatus;

    @Schema(description = "资产状况名称", example = "良好")
    private String conditionStatusName;

    @Schema(description = "审批状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer approvalStatus;

    @Schema(description = "审批状态名称", example = "待审批")
    private String approvalStatusName;

    @Schema(description = "BMP流程状态", example = "1")
    private Integer bmpStatus;

    @Schema(description = "BMP流程状态名称", example = "流程中")
    private String bmpStatusName;

    @Schema(description = "BMP流程实例ID", example = "proc_inst_123")
    private String bmpProcessInstanceId;

    @Schema(description = "审批通过后创建的资产ID", example = "100")
    private Long assetId;

    @Schema(description = "审批人用户ID", example = "1")
    private Long approverUserId;

    @Schema(description = "审批人姓名", example = "张三")
    private String approverUserName;

    @Schema(description = "审批时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private LocalDateTime approvalTime;

    @Schema(description = "审批备注", example = "同意录入")
    private String approvalRemark;

    @Schema(description = "拒绝原因", example = "价格过高")
    private String rejectReason;

    @Schema(description = "备注", example = "申请录入IT设备")
    private String remark;

    @Schema(description = "创建者", example = "admin")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新者", example = "admin")
    private String updater;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private LocalDateTime updateTime;

} 