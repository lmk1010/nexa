package com.kyx.service.business.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "RPC 服务 - 入职用户创建 Request DTO")
@Data
public class UserOnboardingCreateReqDTO {

    @Schema(description = "员工姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "员工姓名不能为空")
    private String employeeName;

    @Schema(description = "手机号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "手机号码不能为空")
    private String mobile;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "部门ID", example = "1")
    private Long deptId;

    @Schema(description = "部门名称", example = "技术部")
    private String deptName;

    @Schema(description = "职位", example = "软件工程师")
    private String position;

    @Schema(description = "性别", example = "1")
    private Integer sex;

    @Schema(description = "入职编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ON20250120001")
    @NotBlank(message = "入职编号不能为空")
    private String onboardingNo;
} 