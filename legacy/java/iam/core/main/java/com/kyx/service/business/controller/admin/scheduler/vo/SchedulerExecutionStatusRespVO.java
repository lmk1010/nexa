package com.kyx.service.business.controller.admin.scheduler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 定时任务执行状态响应 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 定时任务执行状态响应")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerExecutionStatusRespVO {

    @Schema(description = "日志ID", required = true, example = "1024")
    private Long logId;

    @Schema(description = "任务ID", required = true, example = "1")
    private Long taskId;

    @Schema(description = "任务名称", required = true, example = "用户数据同步")
    private String jobName;

    @Schema(description = "执行状态（0成功 1失败 2执行中）", required = true, example = "2")
    private Integer status;

    @Schema(description = "执行进度（0-100）", required = true, example = "50")
    private Integer progress;

    @Schema(description = "进度描述信息", example = "正在处理第100条数据")
    private String progressMessage;

    @Schema(description = "开始时间", required = true)
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "执行耗时（毫秒）")
    private Long duration;

    @Schema(description = "执行结果消息")
    private String resultMessage;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "是否为手动执行", required = true, example = "true")
    private Boolean isManual;

    @Schema(description = "新增记录数", example = "100")
    private Integer insertCount;

    @Schema(description = "更新记录数", example = "50")
    private Integer updateCount;

    @Schema(description = "失败记录数", example = "5")
    private Integer failureCount;

    @Schema(description = "跳过记录数", example = "10")
    private Integer skipCount;

    @Schema(description = "执行统计信息")
    private ExecutionStats stats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutionStats {
        @Schema(description = "总处理记录数")
        private Integer total;

        @Schema(description = "新增记录数")
        private Integer inserted;

        @Schema(description = "更新记录数")
        private Integer updated;

        @Schema(description = "失败记录数")
        private Integer failed;

        @Schema(description = "跳过记录数")
        private Integer skipped;
    }
}