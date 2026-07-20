package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 盘点执行进度 Response VO")
@Data
public class ErpInventoryPlanProgressRespVO {

    @Schema(description = "盘点计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long planId;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "INV-20241201-0001")
    private String planNo;

    @Schema(description = "计划名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024年第一季度全面盘点")
    private String planName;

    @Schema(description = "盘点计划状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer status;

    @Schema(description = "盘点计划状态名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "进行中")
    private String statusName;

    @Schema(description = "完成进度信息")
    private ProgressInfo progressInfo;

    @Schema(description = "执行时间线")
    private List<TimelineItem> timeline;

    @Schema(description = "完成进度信息")
    @Data
    public static class ProgressInfo {
        @Schema(description = "总资产数量", example = "1200")
        private Integer totalAssetCount;

        @Schema(description = "已完成数量", example = "350")
        private Integer completedAssetCount;

        @Schema(description = "完成百分比", example = "29.17")
        private BigDecimal completionPercentage;

        @Schema(description = "待盘点数量", example = "850")
        private Integer pendingAssetCount;

        @Schema(description = "异常数量", example = "15")
        private Integer abnormalAssetCount;
    }

    @Schema(description = "时间线项目")
    @Data
    public static class TimelineItem {
        @Schema(description = "变更日志编号", example = "1")
        private Long id;

        @Schema(description = "变更类型", example = "status_change")
        private String changeType;

        @Schema(description = "变更类型名称", example = "状态变更")
        private String changeTypeName;

        @Schema(description = "原状态", example = "1")
        private Integer oldStatus;

        @Schema(description = "新状态", example = "2")
        private Integer newStatus;

        @Schema(description = "原状态名称", example = "已发布")
        private String oldStatusName;

        @Schema(description = "新状态名称", example = "进行中")
        private String newStatusName;

        @Schema(description = "变更原因", example = "开始执行盘点")
        private String changeReason;

        @Schema(description = "操作人用户ID", example = "1")
        private Long operationUserId;

        @Schema(description = "操作人姓名", example = "张三")
        private String operationUserName;

        @Schema(description = "操作时间", example = "2024-03-01 08:30:00")
        private LocalDateTime operationTime;

        @Schema(description = "备注", example = "盘点负责人启动盘点执行")
        private String remark;
    }
} 