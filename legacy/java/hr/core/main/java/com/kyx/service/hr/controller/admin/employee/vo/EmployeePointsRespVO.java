package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工积分记录 Response VO")
@Data
public class EmployeePointsRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long profileId;

    @Schema(description = "积分类型：1奖励 2扣减 3兑换 4过期")
    private Integer pointsType;

    @Schema(description = "积分数量（正数加分，负数扣分）")
    private BigDecimal points;

    @Schema(description = "变动后余额")
    private BigDecimal balance;

    @Schema(description = "积分原因")
    private String reason;

    @Schema(description = "来源类型：attendance/performance/activity/manual")
    private String sourceType;

    @Schema(description = "来源ID")
    private Long sourceId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
