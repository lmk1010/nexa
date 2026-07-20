package com.kyx.service.business.controller.admin.tenant.vo.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 用户绑定的租户信息 Response VO")
@Data
public class UserTenantRespVO {

    @Schema(description = "租户编号", example = "1024")
    private Long tenantId;

    @Schema(description = "租户名称", example = "芋道科技")
    private String tenantName;

    @Schema(description = "租户状态", example = "0")
    private Integer status;

    @Schema(description = "是否过期", example = "false")
    private Boolean expired;

    @Schema(description = "是否默认租户", example = "true")
    private Boolean isDefault;

    @Schema(description = "是否有角色权限", example = "true")
    private Boolean hasRole;

    @Schema(description = "全局视图（0关闭 1开启）", example = "0")
    private Integer globalView;

    @Schema(description = "数据视角（SELF/GROUP/ALL）", example = "SELF")
    private String viewScope;

} 
