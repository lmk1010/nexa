package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 员工盘点信息 Response VO")
@Data
public class EmployeeInventoryRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "盘点项目名称")
    private String itemName;

    @Schema(description = "盘点项目编号")
    private String itemCode;

    @Schema(description = "盘点状态")
    private String status;

    @Schema(description = "盘点日期")
    private LocalDate checkDate;

    @Schema(description = "备注")
    private String remark;
}
