package com.kyx.service.erp.controller.admin.asset.vo.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 资产附件上传 Request VO")
@Data
public class ErpAssetAttachmentUploadReqVO {

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "上传文件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "上传文件不能为空")
    private MultipartFile file;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "资产购买发票")
    private String remark;

} 