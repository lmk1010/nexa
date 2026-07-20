package com.kyx.service.erp.controller.admin.asset.vo.overview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 资产统计请求 VO")
@Data
public class ErpAssetStatisticsReqVO {

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "分类编号")
    private Long categoryId;

}