package com.kyx.service.business.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "RPC - upsert request for system_user_sync")
@Data
public class UserSyncUpsertReqDTO {

    @Schema(description = "External user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "externalUserId cannot be blank")
    private String externalUserId;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Nickname")
    private String nickname;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Mobile")
    private String mobile;

    @Schema(description = "Department name")
    private String deptName;

    @Schema(description = "Post name")
    private String postName;

    @Schema(description = "Role name")
    private String roleName;

    @Schema(description = "User type")
    private String userType;

    @Schema(description = "Contact name")
    private String linkName;

    @Schema(description = "Working flag: 1 yes, 0 no")
    private Integer worked;

    @Schema(description = "Status: 0 normal, 1 disabled")
    private Integer status;

    @Schema(description = "Sync status: 0 pending, 1 success, 2 failed")
    private Integer syncStatus;

    @Schema(description = "Sync error")
    private String syncError;

    @Schema(description = "Raw external payload")
    private String externalData;
}
