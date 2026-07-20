package com.kyx.service.business.controller.admin.org.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 组织架构树节点 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgTreeNodeRespVO {

    @Schema(description = "节点ID（t-{tenantId} 或 d-{deptId}）", requiredMode = Schema.RequiredMode.REQUIRED, example = "t-1")
    private String id;

    @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "总公司")
    private String name;

    @Schema(description = "父节点ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "t-0")
    private String parentId;

    @Schema(description = "节点类型：tenant-租户，dept-部门", requiredMode = Schema.RequiredMode.REQUIRED, example = "tenant")
    private String nodeType;

    @Schema(description = "租户ID（节点所属租户）", example = "1")
    private Long tenantId;

    @Schema(description = "部门ID（仅 dept 类型有值）", example = "10")
    private Long deptId;

}
