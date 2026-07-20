package com.kyx.service.hr.controller.admin.reminder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Schema(description = "Admin - HR reminder rule save request")
@Data
public class HrReminderRuleSaveReqVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "Rule code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规则编码不能为空")
    @Size(max = 64, message = "规则编码长度不能超过64个字符")
    private String ruleCode;

    @Schema(description = "Rule name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过100个字符")
    private String ruleName;

    @Schema(description = "Business type", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 64, message = "业务类型长度不能超过64个字符")
    private String businessType;

    @Schema(description = "Trigger type", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "触发类型不能为空")
    @Size(max = 64, message = "触发类型长度不能超过64个字符")
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
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

}
