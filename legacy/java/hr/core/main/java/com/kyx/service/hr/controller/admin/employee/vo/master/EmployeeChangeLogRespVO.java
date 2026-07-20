package com.kyx.service.hr.controller.admin.employee.vo.master;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工字段变更日志 Response VO")
@Data
public class EmployeeChangeLogRespVO {

    private Long id;

    private Long profileId;

    private String module;

    private String fieldKey;

    private String fieldName;

    private String beforeValue;

    private String afterValue;

    private String sourceType;

    private Long sourceId;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operationTime;

}
