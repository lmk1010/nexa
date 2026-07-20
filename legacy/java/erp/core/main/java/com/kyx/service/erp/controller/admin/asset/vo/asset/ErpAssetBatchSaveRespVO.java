package com.kyx.service.erp.controller.admin.asset.vo.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - ERP 资产批量新增 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetBatchSaveRespVO {

    @Schema(description = "总数量", example = "10")
    private Integer total;

    @Schema(description = "成功数量", example = "8")
    private Integer successCount;

    @Schema(description = "失败数量", example = "2")
    private Integer failureCount;

    @Schema(description = "成功创建的资产ID列表")
    private List<Long> successIds;

    @Schema(description = "失败信息列表")
    private List<FailureInfo> failures;

    @Schema(description = "失败信息")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureInfo {
        @Schema(description = "索引位置", example = "0")
        private Integer index;
        
        @Schema(description = "资产编码", example = "ASSET001")
        private String assetNo;
        
        @Schema(description = "资产名称", example = "联想电脑")
        private String assetName;
        
        @Schema(description = "错误信息", example = "资产编码已存在")
        private String errorMessage;
    }

} 