package com.kyx.service.hr.controller.admin.reminder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - HR reminder record response")
@Data
public class HrReminderRecordRespVO {

    private Long id;

    private String recordKey;

    private Long ruleId;

    private String ruleCode;

    private String ruleName;

    private String businessType;

    private Long businessId;

    private Long receiverUserId;

    private Long profileId;

    private String employeeName;

    private String title;

    private String content;

    private String severity;

    private String status;

    private String routePath;

    private String sourceType;

    private Long sourceId;

    private LocalDateTime triggerTime;

    private LocalDateTime readTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
