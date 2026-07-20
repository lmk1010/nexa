package com.kyx.service.business.controller.admin.auth.vo;

import com.kyx.service.business.controller.admin.tenant.vo.tenant.UserTenantRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - 社交登录浏览器握手状态 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthSocialHandoffStatusRespVO {

    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";

    @Schema(description = "握手状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "PENDING")
    private String status;

    @Schema(description = "状态消息", example = "已在钉钉完成授权，请返回浏览器继续")
    private String message;

    @Schema(description = "预登录令牌")
    private String preAuthToken;

    @Schema(description = "可选租户列表")
    private List<UserTenantRespVO> tenantList;

    public static AuthSocialHandoffStatusRespVO pending() {
        return AuthSocialHandoffStatusRespVO.builder()
                .status(STATUS_PENDING)
                .message("PENDING")
                .build();
    }

}
