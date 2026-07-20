package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 绩效方案 Response VO")
@Data
public class EmployeePerformanceSchemeRespVO {

    private Long id;

    private String schemeCode;

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

    private LocalDateTime createTime;
}
