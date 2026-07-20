package com.kyx.service.im.controller.admin.tencent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "Admin - Tencent IM user mapping Response VO")
@Data
@Accessors(chain = true)
public class TencentImUserMappingRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "Tenant id")
    private Long tenantId;

    @Schema(description = "OA user id")
    private Long oaUserId;

    @Schema(description = "OA username")
    private String oaUsername;

    @Schema(description = "ordersys user type prefix")
    private String ordersysUserPrefix;

    @Schema(description = "ordersys username")
    private String ordersysUsername;

    @Schema(description = "fixed Tencent IM prefix")
    private String fixedPrefix;

    @Schema(description = "Tencent IM userID")
    private String imUserId;

    @Schema(description = "Status: 0 enabled, 1 disabled")
    private Integer status;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
