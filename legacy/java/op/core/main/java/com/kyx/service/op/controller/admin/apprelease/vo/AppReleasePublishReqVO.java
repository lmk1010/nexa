package com.kyx.service.op.controller.admin.apprelease.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - App 版本发布 Request VO")
@Data
public class AppReleasePublishReqVO {

    @Schema(description = "平台", example = "android")
    @NotBlank(message = "平台不能为空")
    private String platform;

    @Schema(description = "渠道", example = "prod")
    @NotBlank(message = "渠道不能为空")
    private String channel;

    @Schema(description = "版本名", example = "1.0.1")
    @NotBlank(message = "版本名不能为空")
    private String versionName;

    @Schema(description = "版本号", example = "2")
    @NotNull(message = "版本号不能为空")
    @Min(value = 1, message = "版本号必须大于 0")
    private Integer versionCode;

    @Schema(description = "文件 ID")
    private String fileId;

    @Schema(description = "下载地址")
    private String downloadUrl;

    @Schema(description = "SHA-256")
    @NotBlank(message = "SHA-256 不能为空")
    private String sha256;

    @Schema(description = "文件大小")
    @Min(value = 0, message = "文件大小不能小于 0")
    private Long fileSize;

    @Schema(description = "是否强制更新")
    private Boolean forceUpdate;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "更新说明")
    private String releaseNotes;

}
