package com.kyx.service.business.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 租户确认登录 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthTenantLoginReqVO {

    @Schema(description = "预登录令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "8f5ddc10a2f44a54a9f4f8a0e7b7ef79")
    @NotEmpty(message = "预登录令牌不能为空")
    private String preAuthToken;

    @Schema(description = "确认登录的租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "租户编号不能为空")
    private Long tenantId;

    @Schema(description = "设备类型", example = "WEB")
    private String deviceType;

    @Schema(description = "设备标识", example = "device_123456")
    private String deviceId;

}
