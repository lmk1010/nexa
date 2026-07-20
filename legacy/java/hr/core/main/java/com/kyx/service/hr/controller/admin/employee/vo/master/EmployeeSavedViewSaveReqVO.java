package com.kyx.service.hr.controller.admin.employee.vo.master;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 员工花名册保存视图保存 Request VO")
@Data
public class EmployeeSavedViewSaveReqVO {

    private Long id;

    @NotBlank(message = "视图名称不能为空")
    private String viewName;

    private String filterJson;

    private String columnsJson;

    private String sortJson;

    private Boolean defaultView;

}
