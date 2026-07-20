package com.kyx.service.hr.controller.admin.selfservice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 员工自助首页 Response VO")
@Data
public class HrSelfServiceHomeRespVO {

    @Schema(description = "是否已绑定员工档案")
    private Boolean hasProfile;

    @Schema(description = "员工档案摘要")
    private EmployeeProfile profile;

    @Schema(description = "当前任职摘要")
    private Employment employment;

    @Schema(description = "今日考勤摘要")
    private TodayAttendance todayAttendance;

    @Schema(description = "待办摘要")
    private TodoSummary todoSummary;

    @Schema(description = "档案完整度")
    private ProfileHealth profileHealth;

    @Schema(description = "生命周期进展")
    private List<LifecycleItem> lifecycleItems;

    @Schema(description = "快捷入口")
    private List<QuickAction> quickActions;

    @Schema(description = "员工档案摘要")
    @Data
    public static class EmployeeProfile {

        @Schema(description = "员工档案 ID")
        private Long profileId;

        @Schema(description = "系统用户 ID")
        private Long userId;

        @Schema(description = "档案编号")
        private String profileNo;

        @Schema(description = "姓名")
        private String name;

        @Schema(description = "手机号")
        private String mobile;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "入职日期")
        private LocalDate onboardDate;

        @Schema(description = "转正日期")
        private LocalDate confirmationDate;
    }

    @Schema(description = "当前任职摘要")
    @Data
    public static class Employment {

        @Schema(description = "任职记录 ID")
        private Long entryId;

        @Schema(description = "员工编号")
        private String employeeNo;

        @Schema(description = "部门 ID")
        private Long deptId;

        @Schema(description = "职位")
        private String jobTitle;

        @Schema(description = "工作状态")
        private Integer workStatus;

        @Schema(description = "工作状态文本")
        private String workStatusText;

        @Schema(description = "入职日期")
        private LocalDate entryDate;

        @Schema(description = "合同结束日期")
        private LocalDate contractEndDate;
    }

    @Schema(description = "今日考勤摘要")
    @Data
    public static class TodayAttendance {

        @Schema(description = "考勤日期")
        private LocalDate attendanceDate;

        @Schema(description = "上班打卡时间")
        private LocalDateTime clockInTime;

        @Schema(description = "下班打卡时间")
        private LocalDateTime clockOutTime;

        @Schema(description = "上班打卡状态")
        private String clockInStatus;

        @Schema(description = "下班打卡状态")
        private String clockOutStatus;

        @Schema(description = "本月有打卡的天数")
        private Integer monthClockDays;

        @Schema(description = "今日是否请假")
        private Boolean onLeaveToday;

        @Schema(description = "今日是否出差")
        private Boolean onTripToday;
    }

    @Schema(description = "待办摘要")
    @Data
    public static class TodoSummary {

        @Schema(description = "待填写问卷数")
        private Integer pendingQuestionnaireCount;

        @Schema(description = "可参加考试数")
        private Integer availableExamCount;

        @Schema(description = "进行中考试数")
        private Integer inProgressExamCount;

        @Schema(description = "生命周期待办数")
        private Integer pendingLifecycleTaskCount;

        @Schema(description = "统一待办中心待办数")
        private Integer openTodoCount;

        @Schema(description = "审批中请假数")
        private Integer runningLeaveCount;

        @Schema(description = "审批中出差数")
        private Integer runningTripCount;
    }

    @Schema(description = "档案健康度")
    @Data
    public static class ProfileHealth {

        @Schema(description = "完整度百分比")
        private Integer completeness;

        @Schema(description = "缺失字段")
        private List<String> missingFields;
    }

    @Schema(description = "生命周期进展")
    @Data
    public static class LifecycleItem {

        @Schema(description = "事件 ID")
        private Long eventId;

        @Schema(description = "事件类型")
        private String eventType;

        @Schema(description = "事件名称")
        private String eventTypeName;

        @Schema(description = "事件状态")
        private String eventStatus;

        @Schema(description = "事件状态文本")
        private String eventStatusText;

        @Schema(description = "生效日期")
        private LocalDate effectiveDate;
    }

    @Schema(description = "快捷入口")
    @Data
    public static class QuickAction {

        @Schema(description = "标题")
        private String title;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "前端路径")
        private String path;

        @Schema(description = "类别")
        private String category;
    }
}
