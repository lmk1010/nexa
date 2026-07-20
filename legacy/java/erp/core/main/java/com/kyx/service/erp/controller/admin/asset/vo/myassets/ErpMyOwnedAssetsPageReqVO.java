package com.kyx.service.erp.controller.admin.asset.vo.myassets;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 我的拥有资产分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpMyOwnedAssetsPageReqVO extends PageParam {

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想电脑")
    private String name;

    @Schema(description = "资产分类编号", example = "1")
    private Long categoryId;

    @Schema(description = "资产状态", example = "1")
    private Integer status;

    @Schema(description = "所有权状态", example = "1")
    private Integer ownershipStatus;

} 