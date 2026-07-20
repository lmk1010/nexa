package com.kyx.service.erp.controller.admin.asset.vo.overview;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 快过期资产响应 VO")
@Data
public class ErpAssetExpiringRespVO {

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "资产名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公电脑")
    private String name;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "AS001")
    private String assetNo;

    @Schema(description = "资产类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "固定资产")
    private String type;

    @Schema(description = "分类编号")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "规格型号")
    private String specification;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "序列号")
    private String serialNumber;

    @Schema(description = "购买日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime purchaseDate;

    @Schema(description = "购买价格")
    private BigDecimal purchasePrice;

    @Schema(description = "当前价值")
    private BigDecimal currentValue;

    @Schema(description = "质保到期日")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime warrantyDate;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "供应商编号")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "使用状况")
    private Integer conditionStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "剩余天数")
    private Long remainingDays;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}