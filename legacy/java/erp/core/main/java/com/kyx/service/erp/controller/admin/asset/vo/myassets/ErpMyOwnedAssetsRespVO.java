package com.kyx.service.erp.controller.admin.asset.vo.myassets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Schema(description = "管理后台 - 我的拥有资产 Response VO")
@Data
public class ErpMyOwnedAssetsRespVO {

    @Schema(description = "所有权关系编号", example = "1")
    private Long ownershipId;

    @Schema(description = "资产编号", example = "1")
    private Long assetId;

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想电脑")
    private String name;

    @Schema(description = "资产类型", example = "办公设备")
    private String type;

    @Schema(description = "资产分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "资产分类名称", example = "电脑设备")
    private String categoryName;

    @Schema(description = "规格型号", example = "ThinkPad T14")
    private String specification;

    @Schema(description = "品牌", example = "联想")
    private String brand;

    @Schema(description = "型号", example = "T14")
    private String model;

    @Schema(description = "存放位置", example = "办公室A区")
    private String location;

    @Schema(description = "购置价格，单位：元", example = "8000.00")
    private BigDecimal purchasePrice;

    @Schema(description = "当前价值，单位：元", example = "6000.00")
    private BigDecimal currentValue;

    @Schema(description = "资产状态", example = "1")
    private Integer status;

    @Schema(description = "资产状况", example = "1")
    private Integer conditionStatus;

    @Schema(description = "领用记录编号", example = "1")
    private Long checkoutId;

    @Schema(description = "开始使用时间")
    private LocalDateTime startTime;

    @Schema(description = "结束使用时间")
    private LocalDateTime endTime;

    @Schema(description = "所有权状态：1-使用中，2-已归还，3-转移", example = "1")
    private Integer ownershipStatus;

    @Schema(description = "备注", example = "办公使用")
    private String remark;

    @Schema(description = "创建时间")
    private Date createTime;

} 