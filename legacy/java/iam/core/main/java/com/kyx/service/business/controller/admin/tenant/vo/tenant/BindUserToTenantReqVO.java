package com.kyx.service.business.controller.admin.tenant.vo.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 绑定用户到租户 Request VO")
@Data
public class BindUserToTenantReqVO {

    @Schema(description = "用户ID", required = true, example = "1")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "租户ID", required = true, example = "1")
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @Schema(description = "角色ID列表", example = "[1,2]")
    private List<Long> roleIds;

    @Schema(description = "部门ID", example = "1")
    private Long deptId;

    @Schema(description = "岗位ID列表", example = "[1,2]")
    private List<Long> postIds;

    @Schema(description = "是否设为默认租户", example = "false")
    private Boolean isDefault;

}
