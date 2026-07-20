package com.kyx.service.hr.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 入职链接验证 Response VO")
@Data
public class OnboardingLinkValidateRespVO {

    @Schema(description = "链接是否有效", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean valid;

    @Schema(description = "验证失败原因")
    private String message;

    @Schema(description = "剩余有效时间（秒）")
    private Long remainingTime;

    @Schema(description = "入职记录ID")
    private Long entryId;

    @Schema(description = "是否已填写表单")
    private Boolean isSubmitted;

    @Schema(description = "入职状态")
    private Integer onboardingStatus;

    @Schema(description = "入职状态描述")
    private String onboardingStatusDesc;

}