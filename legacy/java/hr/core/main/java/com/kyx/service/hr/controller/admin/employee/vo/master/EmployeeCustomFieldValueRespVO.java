package com.kyx.service.hr.controller.admin.employee.vo.master;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 员工自定义字段值 Response VO")
@Data
public class EmployeeCustomFieldValueRespVO {

    private Long id;

    private Long profileId;

    private Long fieldId;

    private String fieldKey;

    private String fieldName;

    private String fieldType;

    private String fieldGroup;

    private String optionsJson;

    private Boolean requiredFlag;

    private Boolean sensitiveFlag;

    private Boolean maskedFlag;

    private Integer sortOrder;

    private String fieldValue;

    private String valueJson;

}
