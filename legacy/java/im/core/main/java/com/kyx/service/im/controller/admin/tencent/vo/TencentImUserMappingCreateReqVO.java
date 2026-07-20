package com.kyx.service.im.controller.admin.tencent.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "Admin - Tencent IM user mapping create Request VO")
@Data
public class TencentImUserMappingCreateReqVO {

    @Schema(description = "Tenant id", example = "1")
    private Long tenantId;

    @Schema(description = "OA user id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "OA user id cannot be null")
    private Long oaUserId;

    @Schema(description = "OA username", example = "Zhang San")
    private String oaUsername;

    @Schema(description = "ordersys user type prefix", example = "EMPLOYEE")
    private String ordersysUserPrefix;

    @Schema(description = "ordersys username", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "ordersys username cannot be blank")
    private String ordersysUsername;

    @Schema(description = "fixed Tencent IM prefix", example = "OA_")
    private String fixedPrefix;

    @Schema(description = "Tencent IM userID. Empty means auto compose.", example = "EMPLOYEE_OA_admin")
    private String imUserId;

    @Schema(description = "Status: 0 enabled, 1 disabled", example = "0")
    private Integer status;

    @Schema(description = "Remark")
    private String remark;
}
