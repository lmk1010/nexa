package com.kyx.service.hr.controller.admin.employee.vo.master;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工花名册保存视图 Response VO")
@Data
public class EmployeeSavedViewRespVO {

    private Long id;

    private Long userId;

    private String viewName;

    private String filterJson;

    private String columnsJson;

    private String sortJson;

    private Boolean defaultView;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
