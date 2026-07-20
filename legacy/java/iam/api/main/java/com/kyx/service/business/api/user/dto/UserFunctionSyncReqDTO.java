package com.kyx.service.business.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

@Schema(description = "RPC - 用户岗位角色同步 Request DTO")
@Data
public class UserFunctionSyncReqDTO {

    @Schema(description = "用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "userId cannot be null")
    private Long userId;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "本次来源应绑定的角色")
    private List<RoleItem> roles;

    @Schema(description = "由本次来源托管、需要按本次结果替换的角色编码")
    private Set<String> managedRoleCodes;

    @Schema(description = "是否清理 dingtalk_role_ 前缀的历史同步角色")
    private Boolean removeDingTalkGeneratedRoles;

    @Schema(description = "同步来源")
    private String source;

    @Schema(description = "角色项")
    @Data
    public static class RoleItem {

        @Schema(description = "角色编码；为空时按名称复用或自动生成")
        private String code;

        @Schema(description = "角色名称")
        private String name;

        @Schema(description = "不存在时是否自动创建")
        private Boolean createIfMissing;
    }
}
