package com.kyx.service.im.controller.app.tencent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "Mobile - Tencent IM login ticket")
@Data
@Accessors(chain = true)
public class TencentImLoginRespVO {

    @Schema(description = "Tencent IM SDKAppID")
    private Long sdkAppId;

    @Schema(description = "Tencent IM user id")
    private String userID;

    @Schema(description = "Tencent IM UserSig")
    private String userSig;

    @Schema(description = "Expire timestamp in seconds")
    private Long expire;

    @Schema(description = "OA user id")
    private Long oaUserId;

    @Schema(description = "Tenant id")
    private Long tenantId;
}
