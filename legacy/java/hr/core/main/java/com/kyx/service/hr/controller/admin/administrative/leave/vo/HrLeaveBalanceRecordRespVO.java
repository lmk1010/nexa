package com.kyx.service.hr.controller.admin.administrative.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 假期余额流水 Response VO")
@Data
public class HrLeaveBalanceRecordRespVO {

    private Long id;

    private Long balanceId;

    private Long profileId;

    private String profileName;

    private Long userId;

    private String userName;

    private Long leaveTypeId;

    private String leaveTypeCode;

    private String leaveTypeName;

    private Integer year;

    private String changeType;

    private BigDecimal changeAmount;

    private BigDecimal beforeTotalAmount;

    private BigDecimal afterTotalAmount;

    private BigDecimal beforeUsedAmount;

    private BigDecimal afterUsedAmount;

    private BigDecimal beforeFrozenAmount;

    private BigDecimal afterFrozenAmount;

    private BigDecimal beforeRemainAmount;

    private BigDecimal afterRemainAmount;

    private Long operatorId;

    private String operatorName;

    private String sourceType;

    private Long sourceId;

    private String remark;

    private LocalDateTime createTime;

}
