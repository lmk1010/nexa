package com.kyx.service.erp.controller.admin.asset.vo.overview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 用户资产统计响应 VO")
@Data
public class ErpAssetUserStatisticsRespVO {

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "用户姓名")
    private String userName;

    @Schema(description = "资产数量")
    private Long count;

    @Schema(description = "占比（百分比）")
    private BigDecimal percentage;

}