package com.kyx.service.biz.controller.admin.executive.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "Admin - Executive cockpit chat Request VO")
@Data
public class ExecutiveCockpitChatReqVO {

    @Schema(description = "User message", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "消息不能为空")
    private String message;

    @Schema(description = "Conversation id")
    private String conversationId;

    @Schema(description = "Dashboard range days")
    private Integer rangeDays;

}
