package com.kyx.service.business.controller.admin.scheduler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 定时任务统计 Response VO")
@Data
public class SchedulerStatsRespVO {

    @Schema(description = "总执行次数", example = "100")
    private Integer totalCount;

    @Schema(description = "成功次数", example = "95")
    private Integer successCount;

    @Schema(description = "失败次数", example = "5")
    private Integer failedCount;

    @Schema(description = "成功率", example = "95.0")
    private Double successRate;

    @Schema(description = "今日执行次数", example = "10")
    private Integer todayCount;

    @Schema(description = "今日成功次数", example = "9")
    private Integer todaySuccessCount;

    @Schema(description = "今日失败次数", example = "1")
    private Integer todayFailedCount;

}