package com.kyx.service.business.controller.admin.tenant.vo.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 租户树 Response VO")
@Data
public class TenantTreeRespVO {

    @Schema(description = "租户编号", example = "1")
    private Long id;

    @Schema(description = "租户名称", example = "总公司")
    private String name;

    @Schema(description = "父租户编号", example = "0")
    private Long parentId;

    @Schema(description = "集团根租户编号", example = "1")
    private Long rootId;

    @Schema(description = "租户类型（GROUP/COMPANY）", example = "GROUP")
    private String tenantType;

    @Schema(description = "数据视角（SELF/GROUP/ALL）", example = "GROUP")
    private String viewScope;
}
