package com.kyx.service.erp.controller.admin.asset.vo.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - ERP 资产附件批量上传 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssetAttachmentBatchUploadRespVO {

    @Schema(description = "成功上传的附件ID列表")
    private List<Long> successIds;

    @Schema(description = "成功上传数量", example = "5")
    private Integer successCount;

    @Schema(description = "失败上传数量", example = "2")
    private Integer failCount;

    @Schema(description = "失败详情列表")
    private List<UploadFailDetail> failDetails;

    @Schema(description = "上传失败详情")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadFailDetail {
        @Schema(description = "文件名")
        private String fileName;
        
        @Schema(description = "失败原因")
        private String reason;
        
        @Schema(description = "文件大小")
        private Long fileSize;
    }
} 