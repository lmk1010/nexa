package com.kyx.service.business.controller.admin.tenant.vo.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 解绑用户从租户 Request VO")
@Data
public class UnbindUserFromTenantReqVO {

    @Schema(description = "用户ID", required = true, example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "租户ID", required = true, example = "1")
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

}
