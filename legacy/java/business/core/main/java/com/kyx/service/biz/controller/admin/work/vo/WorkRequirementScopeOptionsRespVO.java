package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "Admin - Work Requirement scope options Response VO")
@Data
public class WorkRequirementScopeOptionsRespVO {

    @Schema(description = "当前租户ID", example = "171")
    private Long currentTenantId;

    @Schema(description = "是否开启需求跨租户能力", example = "true")
    private Boolean crossTenantEnabled;

    @Schema(description = "是否具备需求池查询全部权限", example = "true")
    private Boolean queryAllEnabled;

    @Schema(description = "可选租户ID列表")
    private List<Long> selectableTenantIds;

}
