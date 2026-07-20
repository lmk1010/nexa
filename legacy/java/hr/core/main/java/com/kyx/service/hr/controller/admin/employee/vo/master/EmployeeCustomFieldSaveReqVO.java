package com.kyx.service.hr.controller.admin.employee.vo.master;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 员工自定义字段保存 Request VO")
@Data
public class EmployeeCustomFieldSaveReqVO {

    private Long id;

    @NotBlank(message = "字段编码不能为空")
    private String fieldKey;

    @NotBlank(message = "字段名称不能为空")
    private String fieldName;

    @NotBlank(message = "字段类型不能为空")
    private String fieldType;

    private String fieldGroup;

    private String optionsJson;

    private Boolean requiredFlag;

    private Boolean sensitiveFlag;

    private String visibleRoles;

    private Integer sortOrder;

    private Integer status;

}
