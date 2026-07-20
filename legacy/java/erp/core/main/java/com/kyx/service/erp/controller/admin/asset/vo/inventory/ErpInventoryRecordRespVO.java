package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 盘点记录 Response VO")
@Data
public class ErpInventoryRecordRespVO {

    @Schema(description = "盘点记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "盘点计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long planId;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long assetId;

    @Schema(description = "资产编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET-001")
    private String assetCode;

    @Schema(description = "资产名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "笔记本电脑")
    private String assetName;

    @Schema(description = "资产分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "资产分类名称", example = "办公设备")
    private String categoryName;

    @Schema(description = "预期状态", example = "in_use")
    private String expectedStatus;

    @Schema(description = "实际状态", example = "in_use")
    private String actualStatus;

    @Schema(description = "预期位置编号", example = "1")
    private Long expectedLocationId;

    @Schema(description = "预期位置名称", example = "研发部-101")
    private String expectedLocationName;

    @Schema(description = "实际位置编号", example = "1")
    private Long actualLocationId;

    @Schema(description = "实际位置名称", example = "研发部-101")
    private String actualLocationName;

    @Schema(description = "预期使用人编号", example = "1")
    private Long expectedUserId;

    @Schema(description = "预期使用人姓名", example = "张三")
    private String expectedUserName;

    @Schema(description = "实际使用人编号", example = "1")
    private Long actualUserId;

    @Schema(description = "实际使用人姓名", example = "张三")
    private String actualUserName;

    @Schema(description = "盘点结果", example = "normal")
    private String inventoryResult;

    @Schema(description = "盘点结果名称", example = "正常")
    private String inventoryResultName;

    @Schema(description = "盘点时间", example = "2024-03-01 10:30:00")
    private LocalDateTime inventoryTime;

    @Schema(description = "盘点人员编号", example = "1")
    private Long inventoryUserId;

    @Schema(description = "盘点人员姓名", example = "李四")
    private String inventoryUserName;

    @Schema(description = "扫描方式", example = "qr_code")
    private String scanMethod;

    @Schema(description = "扫描内容", example = "QR001")
    private String scanContent;

    @Schema(description = "资产原值", example = "8000.00")
    private BigDecimal originalValue;

    @Schema(description = "资产现值", example = "6000.00")
    private BigDecimal currentValue;

    @Schema(description = "差异说明", example = "位置不符")
    private String diffDescription;

    @Schema(description = "盘点备注", example = "设备运行正常")
    private String remark;

    @Schema(description = "照片URL", example = "http://example.com/photo.jpg")
    private String photoUrl;

    @Schema(description = "是否需要处理", example = "false")
    private Boolean needsAction;

    @Schema(description = "处理状态", example = "completed")
    private String actionStatus;

    @Schema(description = "处理状态名称", example = "已处理")
    private String actionStatusName;

    @Schema(description = "创建时间", example = "2024-03-01 08:00:00")
    private LocalDateTime createTime;

} 