package com.kyx.service.business.controller.admin.tenant.vo.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "管理后台 - 租户用户信息 Response VO")
@Data
public class TenantUserRespVO {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "头像", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "部门ID", example = "1")
    private Long deptId;

    @Schema(description = "部门名称", example = "研发部")
    private String deptName;

    @Schema(description = "岗位ID列表", example = "[1,2]")
    private Set<Long> postIds;

    @Schema(description = "角色ID列表", example = "[1,2]")
    private Set<Long> roleIds;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "是否默认租户", example = "true")
    private Boolean isDefault;

    @Schema(description = "加入时间")
    private LocalDateTime joinTime;

}
