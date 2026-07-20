package com.kyx.service.hr.controller.admin.employee.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class EmployeeMaterialSaveReqVO {

    private Long id;

    @NotNull(message = "员工不能为空")
    private Long profileId;

    @NotBlank(message = "材料分类不能为空")
    private String category;

    private String materialType;

    @NotBlank(message = "材料名称不能为空")
    private String materialName;

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private LocalDate issueDate;

    private LocalDate expireDate;

    private String status;

    private String sourceType;

    private Long sourceId;

    private String remark;

}
