package com.kyx.service.op.controller.admin.apprelease.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "管理后台 - App 版本 Response VO")
@Data
public class AppReleaseRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "平台")
    private String platform;

    @Schema(description = "渠道")
    private String channel;

    @Schema(description = "版本名")
    private String versionName;

    @Schema(description = "版本号")
    private Integer versionCode;

    @Schema(description = "文件 ID")
    private String fileId;

    @Schema(description = "下载地址")
    private String downloadUrl;

    @Schema(description = "SHA-256")
    private String sha256;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "是否强制更新")
    private Boolean forceUpdate;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "更新说明")
    private String releaseNotes;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

}
