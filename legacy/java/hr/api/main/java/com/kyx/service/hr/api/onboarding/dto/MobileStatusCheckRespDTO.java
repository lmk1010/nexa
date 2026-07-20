package com.kyx.service.hr.api.onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPC 服务 - 手机号状态检查 Response DTO")
@Data
public class MobileStatusCheckRespDTO {

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

    @Schema(description = "姓名", example = "员工姓名")
    private String name;

    @Schema(description = "状态类型（1: 办理中 2: 已初始化 3: 已入职 0: 未使用）", example = "1")
    private Integer statusType;

    @Schema(description = "状态描述", example = "该手机号正在办理入职中")
    private String statusDesc;

    @Schema(description = "入职记录ID", example = "1")
    private Long entryId;

    @Schema(description = "员工档案ID", example = "1")
    private Long profileId;

    @Schema(description = "入职编号", example = "ENTRY20250120001")
    private String entryNo;

    @Schema(description = "是否可以直接显示二维码", example = "true")
    private Boolean canShowQRCode;

    @Schema(description = "是否允许创建新账号", example = "false")
    private Boolean canCreateAccount;

    @Schema(description = "用户名", example = "on1234001")
    private String username;

    @Schema(description = "默认密码", example = "kyx123456")
    private String defaultPassword;
}
