package com.kyx.service.hr.controller.admin.employee.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class EmployeeMaterialRenewReqVO {

    @NotNull(message = "材料ID不能为空")
    private Long id;

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private LocalDate issueDate;

    @NotNull(message = "新的到期日期不能为空")
    private LocalDate expireDate;

    private String remark;

}
