package com.kyx.service.im.controller.admin.tencent.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "Admin - Tencent IM user mapping page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TencentImUserMappingPageReqVO extends PageParam {

    @Schema(description = "Tenant id", example = "1")
    private Long tenantId;

    @Schema(description = "OA user id", example = "1024")
    private Long oaUserId;

    @Schema(description = "OA username", example = "Zhang San")
    private String oaUsername;

    @Schema(description = "ordersys user type prefix", example = "EMPLOYEE")
    private String ordersysUserPrefix;

    @Schema(description = "ordersys username", example = "admin")
    private String ordersysUsername;

    @Schema(description = "Tencent IM userID", example = "EMPLOYEE_OA_admin")
    private String imUserId;

    @Schema(description = "Status: 0 enabled, 1 disabled", example = "0")
    private Integer status;
}
