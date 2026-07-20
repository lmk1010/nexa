package com.kyx.service.hr.controller.admin.administrative.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 假期类型 Response VO")
@Data
public class HrLeaveTypeRespVO {

    private Long id;

    private String typeName;

    private String typeCode;

    private Boolean balanceEnabled;

    private String minUnit;

    private Boolean paid;

    private Boolean attachmentRequired;

    private BigDecimal annualDefaultAmount;

    private Integer status;

    private String remark;

}
