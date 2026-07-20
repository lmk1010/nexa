package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - Employee profile change Response VO")
@Data
public class EmployeeProfileChangeRespVO {

    private Long id;

    private Long profileId;

    private String profileName;

    private Long userId;

    private String userNickname;

    private String changeType;

    private String beforeJson;

    private String afterJson;

    private String changeSummary;

    private String reason;

    private String status;

    private String processInstanceId;

    private Long approverId;

    private String approverName;

    private LocalDateTime approvedTime;

    private String approveRemark;

    private LocalDateTime createTime;

}
