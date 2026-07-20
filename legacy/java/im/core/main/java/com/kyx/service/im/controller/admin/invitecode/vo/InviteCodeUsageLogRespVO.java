package com.kyx.service.im.controller.admin.invitecode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Date;

@Schema(description = "管理后台 - 邀请码使用记录 Response VO")
@Data
@Accessors(chain = true)
public class InviteCodeUsageLogRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "邀请码ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long inviteCodeId;

    @Schema(description = "邀请码", requiredMode = Schema.RequiredMode.REQUIRED, example = "INV123456")
    private String inviteCode;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "租户名称", example = "示例公司")
    private String tenantName;

    @Schema(description = "使用者用户ID", example = "1001")
    private Long userId;

    @Schema(description = "使用者用户名", example = "张三")
    private String userName;

    @Schema(description = "使用者IP", example = "192.168.1.100")
    private String userIp;

    @Schema(description = "浏览器UA", example = "Mozilla/5.0...")
    private String userAgent;

    @Schema(description = "设备类型", example = "web")
    private String deviceType;

    @Schema(description = "设备标识", example = "web-001")
    private String deviceId;

    @Schema(description = "使用时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime usageTime;

    @Schema(description = "使用结果：1-成功，0-失败", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer usageResult;

    @Schema(description = "错误信息", example = "邀请码已过期")
    private String errorMessage;

    @Schema(description = "额外数据", example = "{\"key\":\"value\"}")
    private String extraData;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date createTime;
} 