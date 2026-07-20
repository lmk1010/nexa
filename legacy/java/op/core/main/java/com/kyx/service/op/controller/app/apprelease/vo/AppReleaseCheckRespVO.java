package com.kyx.service.op.controller.app.apprelease.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - App 更新检测 Response VO")
@Data
@Accessors(chain = true)
public class AppReleaseCheckRespVO {

    @Schema(description = "是否有更新")
    private Boolean hasUpdate;

    @Schema(description = "最新版本名")
    private String latestVersionName;

    @Schema(description = "最新版本号")
    private Integer latestVersionCode;

    @Schema(description = "下载地址")
    private String downloadUrl;

    @Schema(description = "SHA-256")
    private String sha256;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "是否强制更新")
    private Boolean forceUpdate;

    @Schema(description = "更新说明")
    private String releaseNotes;

    public static AppReleaseCheckRespVO noUpdate() {
        return new AppReleaseCheckRespVO()
                .setHasUpdate(false)
                .setForceUpdate(false);
    }

}
