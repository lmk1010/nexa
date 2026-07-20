package com.kyx.service.im.controller.app.tencent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "Mobile - Tencent IM user id mapping")
@Data
@Accessors(chain = true)
public class TencentImUserIdRespVO {

    @Schema(description = "OA user id")
    private Long oaUserId;

    @Schema(description = "Tenant id")
    private Long tenantId;

    @Schema(description = "Tencent IM user id")
    private String userID;
}
