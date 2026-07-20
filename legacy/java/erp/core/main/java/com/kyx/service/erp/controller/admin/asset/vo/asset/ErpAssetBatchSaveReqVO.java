package com.kyx.service.erp.controller.admin.asset.vo.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - ERP 资产批量新增 Request VO")
@Data
public class ErpAssetBatchSaveReqVO {

    @Schema(description = "资产信息列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "资产信息列表不能为空")
    @Size(min = 1, max = 100, message = "批量保存的资产数量必须在1-100之间")
    @Valid
    private List<ErpAssetSaveReqVO> assets;

} 