package com.kyx.service.erp.controller.admin.asset.vo.overview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 用户资产统计请求 VO")
@Data
public class ErpAssetUserStatisticsReqVO {

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "分类编号")
    private Long categoryId;

    @Schema(description = "限制返回数量，默认20")
    private Integer limit = 20;

}