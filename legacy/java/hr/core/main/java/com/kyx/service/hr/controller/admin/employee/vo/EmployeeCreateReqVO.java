package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工创建 Request VO")
@Data
public class EmployeeCreateReqVO {

    @Schema(description = "员工姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "员工姓名不能为空")
    private String name;

    @Schema(description = "手机号码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号码不能为空")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别（1男 2女）")
    private Integer gender;

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "部门ID不能为空")
    private Long deptId;

    @Schema(description = "职位")
    private String jobTitle;

    @Schema(description = "工作状态（1待入职 2试用期 3在职 4离职）")
    private Integer workStatus;

    @Schema(description = "用工类型（1全职 2兼职 3劳务 4实习）")
    private Integer employmentType;

    @Schema(description = "入职日期")
    private LocalDate onboardDate;

    @Schema(description = "员工状态（1正常 0停用）")
    private Integer status;
}
