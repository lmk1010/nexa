package com.kyx.service.erp.controller.admin.asset.vo.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 资产附件新增/修改 Request VO")
@Data
public class ErpAssetAttachmentSaveReqVO {

    @Schema(description = "附件编号", example = "1")
    private Long id;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "文件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "资产图片.jpg")
    @NotEmpty(message = "文件名称不能为空")
    private String fileName;

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "asset/2024/01/01/image.jpg")
    @NotEmpty(message = "文件路径不能为空")
    private String filePath;

    @Schema(description = "文件访问URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "http://localhost:9000/kyx-files/asset/2024/01/01/image.jpg")
    @NotEmpty(message = "文件URL不能为空")
    private String fileUrl;

    @Schema(description = "文件大小（字节）", example = "1024000")
    private Long fileSize;

    @Schema(description = "文件类型", example = "image/jpeg")
    private String fileType;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "资产购买发票")
    private String remark;

} 