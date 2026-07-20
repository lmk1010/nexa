package com.kyx.service.erp.controller.admin.asset.vo.overview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 部门资产统计请求 VO")
@Data
public class ErpAssetDeptStatisticsReqVO {

    @Schema(description = "分类编号")
    private Long categoryId;

}