package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 员工家庭信息 Response VO")
@Data
public class EmployeeFamilyRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "关系")
    private String relation;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别（1男 2女）")
    private Integer gender;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "工作单位")
    private String workplace;
}
