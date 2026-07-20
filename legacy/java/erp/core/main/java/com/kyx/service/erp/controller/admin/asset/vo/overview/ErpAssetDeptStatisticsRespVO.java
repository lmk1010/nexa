package com.kyx.service.erp.controller.admin.asset.vo.overview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 部门资产统计响应 VO")
@Data
public class ErpAssetDeptStatisticsRespVO {

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "资产数量")
    private Long count;

    @Schema(description = "占比（百分比）")
    private BigDecimal percentage;

}