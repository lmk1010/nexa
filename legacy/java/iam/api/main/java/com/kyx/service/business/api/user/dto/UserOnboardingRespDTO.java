package com.kyx.service.business.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPC 服务 - 入职用户创建 Response DTO")
@Data
public class UserOnboardingRespDTO {

    @Schema(description = "用户ID", example = "1024")
    private Long userId;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "手机号码", example = "13800138000")
    private String mobile;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "部门ID", example = "1")
    private Long deptId;

    @Schema(description = "部门名称", example = "技术部")
    private String deptName;

    @Schema(description = "职位", example = "软件工程师")
    private String position;

    @Schema(description = "默认密码", example = "kyx123456")
    private String defaultPassword;

    @Schema(description = "入职编号", example = "ON20250120001")
    private String onboardingNo;

    @Schema(description = "入职记录ID", example = "1")
    private Long entryId;
} 