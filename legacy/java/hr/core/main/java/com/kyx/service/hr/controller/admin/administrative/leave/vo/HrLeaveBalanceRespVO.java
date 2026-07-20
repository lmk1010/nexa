package com.kyx.service.hr.controller.admin.administrative.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 假期余额 Response VO")
@Data
public class HrLeaveBalanceRespVO {

    private Long id;

    private Long profileId;

    private String profileName;

    private Long userId;

    private String userName;

    private Long leaveTypeId;

    private String leaveTypeCode;

    private String leaveTypeName;

    private Integer year;

    private BigDecimal totalAmount;

    private BigDecimal usedAmount;

    private BigDecimal frozenAmount;

    private BigDecimal remainAmount;

    private String remark;

}
