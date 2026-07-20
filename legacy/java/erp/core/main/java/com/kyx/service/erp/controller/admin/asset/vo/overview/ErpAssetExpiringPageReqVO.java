package com.kyx.service.erp.controller.admin.asset.vo.overview;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - ERP 快过期资产分页请求 VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpAssetExpiringPageReqVO extends PageParam {

    @Schema(description = "多少天内过期，默认90天")
    private Integer days = 90;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "分类编号")
    private Long categoryId;

}