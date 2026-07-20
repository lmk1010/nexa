package com.kyx.service.hr.controller.admin.employee.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 绩效方案分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePerformanceSchemePageReqVO extends PageParam {

    private String schemeName;

    private String schemeCode;

    private String schemeType;

    private String cycleType;

    private String status;

    private Boolean defaultFlag;
}
