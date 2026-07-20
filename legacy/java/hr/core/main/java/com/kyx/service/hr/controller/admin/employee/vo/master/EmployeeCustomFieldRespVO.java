package com.kyx.service.hr.controller.admin.employee.vo.master;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工自定义字段 Response VO")
@Data
public class EmployeeCustomFieldRespVO {

    private Long id;

    private String fieldKey;

    private String fieldName;

    private String fieldType;

    private String fieldGroup;

    private String optionsJson;

    private Boolean requiredFlag;

    private Boolean sensitiveFlag;

    private String visibleRoles;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
