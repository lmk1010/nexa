package com.kyx.service.im.controller.admin.invitecode.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 邀请码使用记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InviteCodeUsageLogPageReqVO extends PageParam {

    @Schema(description = "邀请码ID", example = "1")
    private Long inviteCodeId;

    @Schema(description = "邀请码", example = "INV123456")
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

    @Schema(description = "设备类型", example = "web")
    private String deviceType;

    @Schema(description = "使用结果：1-成功，0-失败", example = "1")
    private Integer usageResult;

    @Schema(description = "使用时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] usageTime;
} 