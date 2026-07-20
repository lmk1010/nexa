package com.kyx.service.business.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Schema(description = "RPC - 用户岗位角色同步 Response DTO")
@Data
public class UserFunctionSyncRespDTO {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "同步后的岗位 ID")
    private Long postId;

    @Schema(description = "同步后的角色 ID")
    private Set<Long> roleIds;

    @Schema(description = "是否新建岗位")
    private Boolean postCreated;

    @Schema(description = "新建角色数量")
    private Integer createdRoleCount;

    @Schema(description = "新增绑定角色数量")
    private Integer assignedRoleCount;

    @Schema(description = "移除托管角色数量")
    private Integer removedRoleCount;
}
