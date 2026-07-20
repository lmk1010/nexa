package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工积分账户 Response VO")
@Data
public class EmployeePointsAccountRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long profileId;

    @Schema(description = "累计获得积分")
    private BigDecimal totalPoints;

    @Schema(description = "已使用积分")
    private BigDecimal usedPoints;

    @Schema(description = "已过期积分")
    private BigDecimal expiredPoints;

    @Schema(description = "当前余额")
    private BigDecimal balance;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
