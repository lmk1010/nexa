package com.kyx.service.hr.controller.admin.risk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HR 风险工作台 Response VO")
@Data
public class HrRiskWorkbenchRespVO {

    @Schema(description = "风险摘要")
    private Summary summary;

    @Schema(description = "风险分类")
    private List<RiskCategory> categories;

    @Schema(description = "优先处理事项")
    private List<RiskItem> priorityItems;

    @Schema(description = "快捷入口")
    private List<QuickAction> quickActions;

    @Data
    public static class Summary {

        @Schema(description = "员工档案总数")
        private Integer totalProfiles;

        @Schema(description = "数据质量分")
        private Integer dataQualityScore;

        @Schema(description = "风险总数")
        private Integer totalRiskCount;

        @Schema(description = "高风险数")
        private Integer highRiskCount;

        @Schema(description = "中风险数")
        private Integer mediumRiskCount;

        @Schema(description = "低风险数")
        private Integer lowRiskCount;

        @Schema(description = "合同到期数")
        private Integer contractExpiringCount;

        @Schema(description = "待转正数")
        private Integer probationDueCount;

        @Schema(description = "绩效预警数")
        private Integer performanceWarningCount;

        @Schema(description = "开放待办数")
        private Integer openTodoCount;

        @Schema(description = "逾期待办数")
        private Integer overdueTodoCount;

        @Schema(description = "高优先级待办数")
        private Integer highPriorityTodoCount;

        @Schema(description = "最新绩效平均分")
        private BigDecimal avgPerformanceScore;

        @Schema(description = "待处理风险事件数")
        private Integer openEventCount;

        @Schema(description = "处理中风险事件数")
        private Integer processingEventCount;

        @Schema(description = "已解决风险事件数")
        private Integer resolvedEventCount;

        @Schema(description = "已忽略风险事件数")
        private Integer ignoredEventCount;
    }

    @Data
    public static class RiskCategory {

        @Schema(description = "分类编码")
        private String code;

        @Schema(description = "分类名称")
        private String name;

        @Schema(description = "风险等级")
        private String severity;

        @Schema(description = "数量")
        private Integer count;

        @Schema(description = "说明")
        private String description;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "路由")
        private String routePath;
    }

    @Data
    public static class RiskItem {

        @Schema(description = "风险事件 ID")
        private Long id;

        @Schema(description = "来源类型")
        private String sourceType;

        @Schema(description = "来源唯一键")
        private String sourceKey;

        @Schema(description = "风险等级")
        private String severity;

        @Schema(description = "问题类型")
        private String issueType;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "说明")
        private String description;

        @Schema(description = "建议动作")
        private String action;

        @Schema(description = "员工档案 ID")
        private Long profileId;

        @Schema(description = "员工姓名")
        private String employeeName;

        @Schema(description = "手机号")
        private String mobile;

        @Schema(description = "路由")
        private String routePath;

        @Schema(description = "到期时间")
        private LocalDateTime dueTime;

        @Schema(description = "处理状态")
        private String status;
    }

    @Data
    public static class QuickAction {

        @Schema(description = "标题")
        private String title;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "路由")
        private String path;
    }
}
