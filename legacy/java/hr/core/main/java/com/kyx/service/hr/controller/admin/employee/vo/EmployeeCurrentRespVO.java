package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 当前登录用户员工档案 Response VO")
@Data
public class EmployeeCurrentRespVO {

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "系统用户ID")
    private Long userId;

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;
}
