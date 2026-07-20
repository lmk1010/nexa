package com.kyx.service.erp.controller.admin.asset.vo.overview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 资产状态统计响应 VO")
@Data
public class ErpAssetStatusStatisticsRespVO {

    @Schema(description = "状态值")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "资产数量")
    private Long count;

    @Schema(description = "占比（百分比）")
    private BigDecimal percentage;

}