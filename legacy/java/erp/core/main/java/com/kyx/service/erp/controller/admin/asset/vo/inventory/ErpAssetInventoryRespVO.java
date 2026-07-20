package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 资产盘点响应 Response VO")
@Data
public class ErpAssetInventoryRespVO {

    @Schema(description = "盘点记录编号", example = "1024")
    private Long id;

    @Schema(description = "盘点计划编号", example = "1")
    private Long planId;

    @Schema(description = "资产编号", example = "1")
    private Long assetId;

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetCode;

    @Schema(description = "资产名称", example = "联想ThinkPad笔记本电脑")
    private String assetName;

    @Schema(description = "资产分类名称", example = "办公设备")
    private String categoryName;

    @Schema(description = "预期状态", example = "normal")
    private String expectedStatus;

    @Schema(description = "实际状态", example = "normal")
    private String actualStatus;

    @Schema(description = "预期位置名称", example = "办公室A101")
    private String expectedLocationName;

    @Schema(description = "实际位置名称", example = "办公室A102")
    private String actualLocationName;

    @Schema(description = "预期使用人姓名", example = "张三")
    private String expectedUserName;

    @Schema(description = "实际使用人姓名", example = "李四")
    private String actualUserName;

    @Schema(description = "盘点结果", example = "normal")
    private String inventoryResult; // normal-正常，status_diff-状态差异，location_diff-位置差异，user_diff-使用人差异，not_found-未找到

    @Schema(description = "盘点时间", example = "2024-01-15 10:30:00")
    private LocalDateTime inventoryTime;

    @Schema(description = "盘点人员姓名", example = "王五")
    private String inventoryUserName;

    @Schema(description = "扫描方式", example = "qr_code")
    private String scanMethod;

    @Schema(description = "扫描内容", example = "ASSET001")
    private String scanContent;

    @Schema(description = "资产原值", example = "8000.00")
    private BigDecimal originalValue;

    @Schema(description = "资产现值", example = "5000.00")
    private BigDecimal currentValue;

    @Schema(description = "差异说明", example = "位置发生变更")
    private String diffDescription;

    @Schema(description = "盘点备注", example = "设备正常运行")
    private String remark;

    @Schema(description = "照片URL", example = "https://example.com/photo.jpg")
    private String photoUrl;

    @Schema(description = "是否需要处理", example = "false")
    private Boolean needsAction;

    @Schema(description = "处理状态", example = "pending")
    private String actionStatus;

    @Schema(description = "创建时间", example = "2024-01-15 10:30:00")
    private LocalDateTime createTime;

} 