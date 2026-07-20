package com.kyx.service.business.controller.admin.scheduler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 定时任务执行日志 Response VO")
@Data
public class SchedulerLogRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "岗位同步")
    private String jobName;

    @Schema(description = "任务类名", requiredMode = Schema.RequiredMode.REQUIRED, example = "SyncScheduleJob")
    private String jobClass;

    @Schema(description = "任务方法名", requiredMode = Schema.RequiredMode.REQUIRED, example = "executePostSync")
    private String jobMethod;

    @Schema(description = "任务参数", example = "{}")
    private String jobParams;

    @Schema(description = "Cron表达式", example = "0 0 */4 * * ?")
    private String cronExpression;

    @Schema(description = "执行状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "状态描述", example = "成功")
    private String statusDesc;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "执行耗时（毫秒）", example = "1500")
    private Long duration;

    @Schema(description = "执行结果消息", example = "同步完成，成功10条")
    private String resultMessage;

    @Schema(description = "错误信息", example = "连接超时")
    private String errorMessage;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "租户名称", example = "集团租户")
    private String tenantName;

}
