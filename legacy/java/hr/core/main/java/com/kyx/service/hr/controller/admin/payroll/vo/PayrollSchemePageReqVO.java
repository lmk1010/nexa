package com.kyx.service.hr.controller.admin.payroll.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 薪资方案分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollSchemePageReqVO extends PageParam {

    private String schemeName;

    private String schemeCode;

    private String status;

    private Boolean defaultFlag;

}
