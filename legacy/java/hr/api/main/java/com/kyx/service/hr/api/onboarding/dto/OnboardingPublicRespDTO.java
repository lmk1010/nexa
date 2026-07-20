package com.kyx.service.hr.api.onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - 公共入职申请 Response DTO")
@Data
public class OnboardingPublicRespDTO {

    @Schema(description = "入职申请ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "申请编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ON20250120001")
    private String applicationNo;

    @Schema(description = "员工档案ID", example = "1001")
    private Long profileId;

    @Schema(description = "档案编号", example = "PROF001")
    private String profileNo;

    @Schema(description = "入职记录ID", example = "2001")
    private Long entryId;

    @Schema(description = "入职编号", example = "ENTRY001")
    private String entryNo;

    @Schema(description = "员工编号", example = "EMP001")
    private String employeeNo;

    @Schema(description = "审批类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer approvalType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-01-20 09:00:00")
    private LocalDateTime createTime;

} 