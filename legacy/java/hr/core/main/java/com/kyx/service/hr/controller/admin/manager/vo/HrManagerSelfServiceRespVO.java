package com.kyx.service.hr.controller.admin.manager.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 经理自助首页 Response VO")
@Data
public class HrManagerSelfServiceRespVO {

    @Schema(description = "是否存在当前登录人的员工档案")
    private Boolean hasProfile;

    @Schema(description = "主管档案")
    private ManagerProfile manager;

    @Schema(description = "团队摘要")
    private Summary summary;

    @Schema(description = "团队考勤摘要")
    private AttendanceSummary attendanceSummary;

    @Schema(description = "团队人员提醒摘要")
    private ReminderSummary reminderSummary;

    @Schema(description = "团队学习摘要")
    private LearningSummary learningSummary;

    @Schema(description = "团队审批摘要")
    private ApprovalSummary approvalSummary;

    @Schema(description = "团队成员")
    private List<TeamMember> teamMembers;

    @Schema(description = "关注事项")
    private List<AttentionItem> attentionItems;

    @Schema(description = "快捷入口")
    private List<QuickAction> quickActions;

    @Schema(description = "团队分析")
    private Analysis analysis;

    @Data
    public static class ManagerProfile {

        @Schema(description = "员工档案 ID")
        private Long profileId;

        @Schema(description = "用户 ID")
        private Long userId;

        @Schema(description = "姓名")
        private String name;

        @Schema(description = "手机号")
        private String mobile;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "部门 ID")
        private Long deptId;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "岗位")
        private String jobTitle;
    }

    @Data
    public static class Summary {

        @Schema(description = "团队人数")
        private Integer teamCount;

        @Schema(description = "在职人数")
        private Integer activeCount;

        @Schema(description = "试用人数")
        private Integer probationCount;

        @Schema(description = "本月入职人数")
        private Integer onboardThisMonthCount;

        @Schema(description = "合同 60 天内到期人数")
        private Integer contractExpiringCount;

        @Schema(description = "绩效关注人数")
        private Integer performanceWarningCount;

        @Schema(description = "开放待办数")
        private Integer openTodoCount;

        @Schema(description = "覆盖部门数")
        private Integer deptCount;

        @Schema(description = "最新绩效平均分")
        private BigDecimal avgPerformanceScore;
    }

    @Data
    public static class AttendanceSummary {

        @Schema(description = "统计开始日期")
        private LocalDate rangeStartDate;

        @Schema(description = "统计结束日期")
        private LocalDate rangeEndDate;

        @Schema(description = "日考勤结果数")
        private Integer dailyResultCount;

        @Schema(description = "异常天数")
        private Integer abnormalDayCount;

        @Schema(description = "异常记录数")
        private Integer exceptionCount;

        @Schema(description = "待处理异常数")
        private Integer pendingExceptionCount;

        @Schema(description = "待审批更正数")
        private Integer pendingCorrectionCount;

        @Schema(description = "迟到次数")
        private Integer lateCount;

        @Schema(description = "早退次数")
        private Integer earlyLeaveCount;

        @Schema(description = "缺卡次数")
        private Integer missingClockCount;

        @Schema(description = "旷工次数")
        private Integer absenteeismCount;

        @Schema(description = "缺勤小时")
        private BigDecimal absentHours;

        @Schema(description = "请假小时")
        private BigDecimal leaveHours;

        @Schema(description = "出差小时")
        private BigDecimal tripHours;
    }

    @Data
    public static class ReminderSummary {

        @Schema(description = "30 天内试用到期人数")
        private Integer probationDueCount;

        @Schema(description = "60 天内合同到期人数")
        private Integer contractExpiringCount;

        @Schema(description = "30 天内材料到期数")
        private Integer materialExpiringCount;

        @Schema(description = "30 天内生日人数")
        private Integer birthdayIn30DaysCount;

        @Schema(description = "本月入职人数")
        private Integer onboardThisMonthCount;

        @Schema(description = "关键资料缺失人数")
        private Integer profileMissingCount;
    }

    @Data
    public static class LearningSummary {

        @Schema(description = "培训任务数")
        private Integer assignmentCount;

        @Schema(description = "已完成任务数")
        private Integer completedCount;

        @Schema(description = "进行中任务数")
        private Integer inProgressCount;

        @Schema(description = "未开始任务数")
        private Integer notStartedCount;

        @Schema(description = "开放任务数")
        private Integer openCount;

        @Schema(description = "平均进度")
        private BigDecimal avgProgress;

        @Schema(description = "完成率")
        private BigDecimal completionRate;
    }

    @Data
    public static class ApprovalSummary {

        @Schema(description = "开放待办数")
        private Integer openTodoCount;

        @Schema(description = "高优先级待办数")
        private Integer highPriorityTodoCount;

        @Schema(description = "逾期待办数")
        private Integer overdueTodoCount;

        @Schema(description = "流转中请假数")
        private Integer runningLeaveCount;

        @Schema(description = "待审批补卡/外勤数")
        private Integer pendingCorrectionCount;

        @Schema(description = "待审批加班数")
        private Integer pendingOvertimeCount;

        @Schema(description = "流转中出差数")
        private Integer runningTripCount;

        @Schema(description = "审批事项合计")
        private Integer workflowPendingCount;
    }

    @Data
    public static class TeamMember {

        @Schema(description = "员工档案 ID")
        private Long profileId;

        @Schema(description = "入职记录 ID")
        private Long entryId;

        @Schema(description = "用户 ID")
        private Long userId;

        @Schema(description = "员工编号")
        private String employeeNo;

        @Schema(description = "姓名")
        private String name;

        @Schema(description = "手机号")
        private String mobile;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "部门 ID")
        private Long deptId;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "岗位")
        private String jobTitle;

        @Schema(description = "工作状态")
        private Integer workStatus;

        @Schema(description = "工作状态文本")
        private String workStatusText;

        @Schema(description = "用工类型")
        private Integer employmentType;

        @Schema(description = "用工类型文本")
        private String employmentTypeText;

        @Schema(description = "入职日期")
        private LocalDate entryDate;

        @Schema(description = "合同结束日期")
        private LocalDate contractEndDate;

        @Schema(description = "最新绩效周期")
        private String latestPerformancePeriod;

        @Schema(description = "最新绩效得分")
        private BigDecimal latestPerformanceScore;

        @Schema(description = "最新绩效等级")
        private String latestPerformanceGrade;

        @Schema(description = "最新绩效结果")
        private String latestPerformanceResult;

        @Schema(description = "最新绩效日期")
        private LocalDate latestPerformanceDate;

        @Schema(description = "开放待办数")
        private Integer openTodoCount;

        @Schema(description = "本月考勤异常数")
        private Integer attendanceExceptionCount;

        @Schema(description = "待审批更正数")
        private Integer pendingCorrectionCount;

        @Schema(description = "培训任务数")
        private Integer trainingAssignmentCount;

        @Schema(description = "培训完成数")
        private Integer trainingCompletedCount;

        @Schema(description = "30 天内材料到期数")
        private Integer materialExpiringCount;

        @Schema(description = "试用到期日期")
        private LocalDate probationDueDate;

        @Schema(description = "即将到来的生日")
        private LocalDate birthdayDate;

        @Schema(description = "风险等级")
        private String riskLevel;

        @Schema(description = "风险原因")
        private String riskReason;
    }

    @Data
    public static class AttentionItem {

        @Schema(description = "关注类型")
        private String type;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "内容")
        private String content;

        @Schema(description = "员工档案 ID")
        private Long profileId;

        @Schema(description = "员工姓名")
        private String employeeName;

        @Schema(description = "路由")
        private String routePath;

        @Schema(description = "优先级")
        private String priority;

        @Schema(description = "到期时间")
        private LocalDateTime dueTime;
    }

    @Data
    public static class QuickAction {

        @Schema(description = "标题")
        private String title;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "路由")
        private String path;

        @Schema(description = "分类")
        private String category;
    }

    @Data
    public static class Analysis {

        @Schema(description = "部门分布")
        private List<DistributionItem> deptDistribution;

        @Schema(description = "风险分布")
        private List<DistributionItem> riskDistribution;

        @Schema(description = "待办分布")
        private List<DistributionItem> todoDistribution;

        @Schema(description = "绩效分布")
        private List<DistributionItem> performanceDistribution;

        @Schema(description = "培训分布")
        private List<DistributionItem> trainingDistribution;

        @Schema(description = "重点关注成员")
        private List<AttentionMember> topAttentionMembers;
    }

    @Data
    public static class DistributionItem {

        @Schema(description = "分组键")
        private String key;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "数量")
        private Integer count;

        @Schema(description = "占比")
        private BigDecimal percent;

        @Schema(description = "跳转路由")
        private String routePath;
    }

    @Data
    public static class AttentionMember {

        @Schema(description = "员工档案 ID")
        private Long profileId;

        @Schema(description = "姓名")
        private String name;

        @Schema(description = "部门名称")
        private String deptName;

        @Schema(description = "岗位")
        private String jobTitle;

        @Schema(description = "风险等级")
        private String riskLevel;

        @Schema(description = "风险原因")
        private String riskReason;

        @Schema(description = "开放待办数")
        private Integer openTodoCount;

        @Schema(description = "考勤异常数")
        private Integer attendanceExceptionCount;

        @Schema(description = "待审批更正数")
        private Integer pendingCorrectionCount;

        @Schema(description = "材料到期数")
        private Integer materialExpiringCount;

        @Schema(description = "培训未完成数")
        private Integer trainingOpenCount;

        @Schema(description = "关注分")
        private Integer attentionScore;

        @Schema(description = "跳转路由")
        private String routePath;
    }
}
