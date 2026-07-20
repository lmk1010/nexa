package com.kyx.service.hr.controller.admin.reminder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - HR reminder rule response")
@Data
public class HrReminderRuleRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "Rule code")
    private String ruleCode;

    @Schema(description = "Rule name")
    private String ruleName;

    @Schema(description = "Business type")
    private String businessType;

    @Schema(description = "Trigger type")
    private String triggerType;

    @Schema(description = "Trigger config JSON")
    private String triggerConfigJson;

    @Schema(description = "Receiver config JSON")
    private String receiverConfigJson;

    @Schema(description = "Channel config JSON")
    private String channelConfigJson;

    @Schema(description = "Template ID")
    private Long templateId;

    @Schema(description = "Enabled")
    private Boolean enabled;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;

}
