package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Schema(description = "管理后台 - 绩效方案新增/修改 Request VO")
@Data
public class EmployeePerformanceSchemeSaveReqVO {

    private Long id;

    private String schemeCode;

    @NotBlank(message = "方案名称不能为空")
    private String schemeName;

    private String schemeType;

    private String cycleType;

    private String status;

    private Boolean defaultFlag;

    private String templateJson;

    private String reviewFlowJson;

    private LocalDate effectiveDate;

    private LocalDate expireDate;

    private String remark;
}
