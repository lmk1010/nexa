package com.kyx.service.hr.controller.admin.payroll.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 工资批次分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayrollBatchPageReqVO extends PageParam {

    @Schema(description = "发薪月份 yyyy-MM")
    private String payrollMonth;

    @Schema(description = "状态：DRAFT/PUBLISHED/LOCKED")
    private String status;

}
