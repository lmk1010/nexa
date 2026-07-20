package com.kyx.service.erp.controller.admin.asset.vo.overview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 资产统计响应 VO")
@Data
public class ErpAssetStatisticsRespVO {

    @Schema(description = "资产总数")
    private Long totalAssets;

    @Schema(description = "正常资产数量")
    private Long normalAssets;

    @Schema(description = "借用中资产数量")
    private Long borrowedAssets;

    @Schema(description = "已报废资产数量")
    private Long scrappedAssets;

    @Schema(description = "快过期资产数量（90天内过期）")
    private Long expiringAssets;

    @Schema(description = "维修中资产数量")
    private Long repairingAssets;

}