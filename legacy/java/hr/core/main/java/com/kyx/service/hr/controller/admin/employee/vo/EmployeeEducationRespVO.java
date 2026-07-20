package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 员工学历 Response VO")
@Data
public class EmployeeEducationRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "学历")
    private String education;

    @Schema(description = "学校名称")
    private String schoolName;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "入学时间")
    private LocalDate enrollmentDate;

    @Schema(description = "毕业时间")
    private LocalDate graduationDate;

    @Schema(description = "是否最高学历")
    private Boolean isHighest;
}
