package com.kyx.service.op.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Schema(description = "RPC 服务 - 文件创建 Request DTO")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileCreateReqDTO {

    @Schema(description = "原文件名称", example = "xxx.png")
    private String name;

    @Schema(description = "文件目录", example = "xxx")
    private String directory;

    @Schema(description = "文件的 MIME 类型", example = "image/png")
    private String type;

    @Schema(description = "文件大小", example = "2048")
    private Integer size;

    @Schema(description = "文件内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件内容不能为空")
    private byte[] content;

}
