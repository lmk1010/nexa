package com.kyx.service.hr.controller.admin.employee.vo.master;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class EmployeeCustomFieldValuesSaveReqVO {

    @NotNull(message = "员工档案 ID 不能为空")
    private Long profileId;

    @Valid
    private List<EmployeeCustomFieldValueSaveReqVO> values;

}
