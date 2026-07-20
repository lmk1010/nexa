package com.kyx.service.erp.controller.admin.product.vo.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - ERP 产品分类列表 Request VO")
@Data
@Accessors(chain = true)
public class ErpProductCategoryListReqVO {

    @Schema(description = "分类名称", example = "MK")
    private String name;

    @Schema(description = "开启状态", example = "1")
    private Integer status;

}