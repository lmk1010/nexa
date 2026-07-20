package com.kyx.service.hr.controller.admin.payroll.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工资批次 Response VO")
@Data
public class PayrollBatchRespVO {

    private Long id;

    private String payrollMonth;

    private String batchName;

    private String status;

    private String processInstanceId;

    private Integer approvalStatus;

    private LocalDateTime generatedTime;

    private LocalDateTime publishedTime;

    private Long publishedBy;

    private LocalDateTime lockedTime;

    private Long lockedBy;

    private String summaryJson;

}
