package com.kyx.service.erp.controller.admin.asset.vo.attachment;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 资产附件 Response VO")
@Data
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class ErpAssetAttachmentRespVO {

    @Schema(description = "附件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("附件编号")
    private Long id;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("资产编号")
    private Long assetId;

    @Schema(description = "文件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "资产图片.jpg")
    @ExcelProperty("文件名称")
    private String fileName;

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "asset/2024/01/01/image.jpg")
    private String filePath;

    @Schema(description = "文件ID", example = "abc123def456789")
    private String fileId; // 新增：文件ID字段（OP服务返回的UUID，通常存储在filePath中）

    @Schema(description = "文件访问URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "http://localhost:9000/kyx-files/asset/2024/01/01/image.jpg")
    @ExcelProperty("文件URL")
    private String fileUrl;

    @Schema(description = "文件大小（字节）", example = "1024000")
    @ExcelProperty("文件大小")
    private Long fileSize;

    @Schema(description = "文件类型", example = "image/jpeg")
    @ExcelProperty("文件类型")
    private String fileType;

    @Schema(description = "排序", example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "备注", example = "资产购买发票")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 