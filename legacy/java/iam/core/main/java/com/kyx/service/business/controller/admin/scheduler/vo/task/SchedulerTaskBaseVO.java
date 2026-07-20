package com.kyx.service.business.controller.admin.scheduler.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 定时任务管理 Base VO，提供给添加、修改、详细的子 VO 使用
 * 如果子 VO 存在差异的字段，请不要添加到这里，影响 Swagger 文档生成
 */
@Data
public class SchedulerTaskBaseVO {

    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "用户数据同步")
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    @Schema(description = "任务描述", example = "从外部系统同步用户数据到本地系统")
    private String taskDescription;

    @Schema(description = "任务类名", requiredMode = Schema.RequiredMode.REQUIRED, example = "com.kyx.service.business.service.sync.UnifiedSyncServiceImpl")
    @NotBlank(message = "任务类名不能为空")
    private String taskClass;

    @Schema(description = "任务方法名", requiredMode = Schema.RequiredMode.REQUIRED, example = "syncUsers")
    @NotBlank(message = "任务方法名不能为空")
    private String taskMethod;

    @Schema(description = "任务参数", example = "{}")
    private String taskParams;

    @Schema(description = "Cron表达式", example = "0 0 2 * * ?")
    private String cronExpression;

    @Schema(description = "任务状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "任务状态不能为空")
    private Integer taskStatus;

    @Schema(description = "任务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "SYNC")
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    @Schema(description = "最后执行时间")
    private LocalDateTime lastExecuteTime;

    @Schema(description = "下次执行时间")
    private LocalDateTime nextExecuteTime;

    @Schema(description = "执行次数", example = "10")
    private Long executeCount;

    @Schema(description = "成功次数", example = "8")
    private Long successCount;

    @Schema(description = "失败次数", example = "2")
    private Long failCount;

}