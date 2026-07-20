package com.kyx.service.hr.controller.admin.employee.vo.master;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class EmployeeCustomFieldValueSaveReqVO {

    @NotNull(message = "字段 ID 不能为空")
    private Long fieldId;

    private String fieldValue;

    private String valueJson;

}
