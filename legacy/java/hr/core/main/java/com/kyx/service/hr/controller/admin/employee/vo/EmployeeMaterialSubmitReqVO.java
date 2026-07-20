package com.kyx.service.hr.controller.admin.employee.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class EmployeeMaterialSubmitReqVO {

    private Long id;

    private String category;

    private String materialType;

    private String materialName;

    @NotBlank(message = "文件链接不能为空")
    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private LocalDate issueDate;

    private LocalDate expireDate;

    private String remark;

}
