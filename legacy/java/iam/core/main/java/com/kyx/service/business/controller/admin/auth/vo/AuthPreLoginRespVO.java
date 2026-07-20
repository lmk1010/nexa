package com.kyx.service.business.controller.admin.auth.vo;

import com.kyx.service.business.controller.admin.tenant.vo.tenant.UserTenantRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 预登录 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthPreLoginRespVO {

    @Schema(description = "预登录令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "8f5ddc10a2f44a54a9f4f8a0e7b7ef79")
    private String preAuthToken;

    @Schema(description = "可选租户列表")
    private List<UserTenantRespVO> tenantList;

}
