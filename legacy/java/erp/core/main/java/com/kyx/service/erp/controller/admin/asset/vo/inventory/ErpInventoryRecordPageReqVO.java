package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 盘点记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpInventoryRecordPageReqVO extends PageParam {

    @Schema(description = "盘点计划编号", example = "1")
    private Long planId;

    @Schema(description = "资产编码", example = "ASSET-001")
    private String assetCode;

    @Schema(description = "资产名称", example = "笔记本电脑")
    private String assetName;

    @Schema(description = "盘点结果", example = "normal")
    private String inventoryResult;

    @Schema(description = "是否需要处理", example = "true")
    private Boolean needsAction;

    @Schema(description = "处理状态", example = "pending")
    private String actionStatus;

} 