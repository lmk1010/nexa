package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 验证扫码响应 VO
 */
@Schema(description = "管理后台 - ERP 验证扫码响应 VO")
@Data
public class ErpValidateScanRespVO {

    @Schema(description = "盘点记录编号", example = "1")
    private Long recordId;

    @Schema(description = "资产编号", example = "1")
    private Long assetId;

    @Schema(description = "资产编码", example = "ASSET49341433746")
    private String assetCode;

    @Schema(description = "资产名称", example = "三星笔记本")
    private String assetName;

    @Schema(description = "资产分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "资产分类名称", example = "机器设备")
    private String categoryName;

    @Schema(description = "预期状态", example = "normal")
    private String expectedStatus;

    @Schema(description = "预期位置编号", example = "1")
    private Long expectedLocationId;

    @Schema(description = "预期位置名称", example = "总部一楼")
    private String expectedLocationName;

    @Schema(description = "预期使用人编号", example = "1")
    private Long expectedUserId;

    @Schema(description = "预期使用人姓名", example = "张三")
    private String expectedUserName;

    @Schema(description = "实际状态", example = "normal")
    private String actualStatus;

    @Schema(description = "实际位置编号", example = "1")
    private Long actualLocationId;

    @Schema(description = "实际位置名称", example = "总部一楼")
    private String actualLocationName;

    @Schema(description = "实际使用人编号", example = "1")
    private Long actualUserId;

    @Schema(description = "实际使用人姓名", example = "张三")
    private String actualUserName;

    @Schema(description = "盘点时间", example = "2023-01-01 12:00:00")
    private LocalDateTime inventoryTime;

    @Schema(description = "盘点人员编号", example = "1")
    private Long inventoryUserId;

    @Schema(description = "盘点人员姓名", example = "盘点员")
    private String inventoryUserName;

    @Schema(description = "扫描方式", example = "qr_code")
    private String scanMethod;

    @Schema(description = "扫描内容", example = "ASSET49341433746")
    private String scanContent;

    @Schema(description = "资产原值", example = "8000.00")
    private BigDecimal originalValue;

    @Schema(description = "资产现值", example = "5000.00")
    private BigDecimal currentValue;

    @Schema(description = "盘点结果", example = "normal")
    private String inventoryResult;

    @Schema(description = "差异说明", example = "")
    private String diffDescription;

    @Schema(description = "备注", example = "")
    private String remark;

    @Schema(description = "照片URL", example = "")
    private String photoUrl;

    @Schema(description = "是否需要处理", example = "false")
    private Boolean needsAction;

    @Schema(description = "处理状态", example = "pending")
    private String actionStatus;

    @Schema(description = "是否已盘点过", example = "false")
    private Boolean alreadyInventoried;
} 