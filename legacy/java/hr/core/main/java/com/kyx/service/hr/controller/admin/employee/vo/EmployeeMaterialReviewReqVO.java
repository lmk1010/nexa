package com.kyx.service.hr.controller.admin.employee.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class EmployeeMaterialReviewReqVO {

    @NotNull(message = "材料ID不能为空")
    private Long id;

    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String rejectReason;

    private String remark;

}
