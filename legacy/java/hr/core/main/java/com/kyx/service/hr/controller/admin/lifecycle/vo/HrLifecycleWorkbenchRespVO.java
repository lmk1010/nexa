package com.kyx.service.hr.controller.admin.lifecycle.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - HR 生命周期工作台 Response VO")
@Data
public class HrLifecycleWorkbenchRespVO {

    @Schema(description = "待入职人数")
    private Integer pendingOnboardingCount;

    @Schema(description = "30 天内待转正人数")
    private Integer probationDueCount;

    @Schema(description = "离职办理中人数")
    private Integer resignPendingCount;

    @Schema(description = "转正办理中人数")
    private Integer regularizationPendingCount;

    @Schema(description = "调岗办理中人数")
    private Integer transferPendingCount;

    @Schema(description = "调薪办理中人数")
    private Integer salaryAdjustPendingCount;

    @Schema(description = "到期待生效事件数")
    private Integer dueEffectiveEventCount;

    @Schema(description = "在职人数")
    private Integer activeEmployeeCount;

    @Schema(description = "已离职人数")
    private Integer resignedEmployeeCount;

    @Schema(description = "本月入职人数")
    private Integer monthOnboardCount;

    @Schema(description = "本月离职人数")
    private Integer monthResignCount;

    @Schema(description = "待处理事件数")
    private Integer pendingEventCount;

    @Schema(description = "待办任务数")
    private Integer pendingTaskCount;

    @Schema(description = "已完成任务数")
    private Integer completedTaskCount;

    @Schema(description = "逾期任务数")
    private Integer overdueTaskCount;

    @Schema(description = "任务完成率")
    private BigDecimal taskCompletionRate = BigDecimal.ZERO;

    @Schema(description = "事件类型分布")
    private List<StatItem> eventTypeStats = new ArrayList<>();

    @Schema(description = "事件状态分布")
    private List<StatItem> eventStatusStats = new ArrayList<>();

    @Schema(description = "任务状态分布")
    private List<StatItem> taskStatusStats = new ArrayList<>();

    @Schema(description = "最近事件")
    private List<HrLifecycleEventRespVO> recentEvents;

    @Schema(description = "逾期任务")
    private List<HrLifecycleTaskRespVO> overdueTasks;

    @Data
    public static class StatItem {

        private String name;

        private Integer count = 0;

        private BigDecimal percent = BigDecimal.ZERO;

        public StatItem() {
        }

        public StatItem(String name, Integer count, BigDecimal percent) {
            this.name = name;
            this.count = count;
            this.percent = percent;
        }
    }

}
