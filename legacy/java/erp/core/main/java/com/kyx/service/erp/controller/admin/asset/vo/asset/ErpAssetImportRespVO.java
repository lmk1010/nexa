package com.kyx.service.erp.controller.admin.asset.vo.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - ERP 资产导入 Response VO")
@Data
@Builder
public class ErpAssetImportRespVO {

    @Schema(description = "创建成功的资产编码数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createAssetNos;

    @Schema(description = "更新成功的资产编码数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> updateAssetNos;

    @Schema(description = "导入失败的资产集合，key 为资产编码，value 为失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureAssetNos;

} 