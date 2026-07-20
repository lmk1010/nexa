package com.kyx.service.erp.controller.admin.asset.vo.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 资产附件批量上传 Request VO")
@Data
public class ErpAssetAttachmentBatchUploadReqVO {

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "上传文件列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "上传文件不能为空")
    private MultipartFile[] files;

    @Schema(description = "起始排序，第一个文件的排序值，后续文件会依次递增", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "资产相关文档")
    private String remark;

} 