package com.kyx.service.erp.controller.admin.asset.vo.overview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 资产分类统计响应 VO")
@Data
public class ErpAssetCategoryStatisticsRespVO {

    @Schema(description = "分类编号")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "资产数量")
    private Long count;

    @Schema(description = "占比（百分比）")
    private BigDecimal percentage;

}