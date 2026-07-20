package com.kyx.service.hr.controller.admin.exam.vo.scope;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "管理后台 - 考试发布范围选项 Response VO")
public class ExamPublishScopeOptionsRespVO {

    @Schema(description = "当前租户编号")
    private Long currentTenantId;

    @Schema(description = "是否支持跨租户发布")
    private Boolean crossTenantEnabled;

    @Schema(description = "可选租户列表")
    private List<TenantOption> tenantList;

    @Schema(description = "组织树节点列表")
    private List<OrgTreeNode> orgTree;

    @Schema(description = "角色选项")
    private List<RoleOption> roleList;

    @Schema(description = "用户选项")
    private List<UserOption> userList;

    @Data
    public static class TenantOption {
        private Long id;
        private String name;
    }

    @Data
    public static class OrgTreeNode {
        private String id;
        private String name;
        private String parentId;
        private String nodeType;
        private Long tenantId;
        private Long deptId;
    }

    @Data
    public static class RoleOption {
        private Long id;
        private String name;
        private Long tenantId;
        private String tenantName;
    }

    @Data
    public static class UserOption {
        private Long id;
        private String username;
        private String nickname;
        private Long tenantId;
        private String tenantName;
        private Long deptId;
        private String deptName;
    }
}
