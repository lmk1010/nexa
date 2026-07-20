package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工培训信息新增/修改 Request VO")
@Data
public class EmployeeTrainingSaveReqVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工档案ID不能为空")
    private Long profileId;

    @Schema(description = "关联课程ID")
    private Long courseId;

    @Schema(description = "关联学习计划ID")
    private Long planId;

    @Schema(description = "关联学习任务ID")
    private Long assignmentId;

    @Schema(description = "培训名称")
    private String trainingName;

    @Schema(description = "培训机构")
    private String provider;

    @Schema(description = "培训开始日期")
    private LocalDate startDate;

    @Schema(description = "培训结束日期")
    private LocalDate endDate;

    @Schema(description = "培训时长（小时）")
    private BigDecimal hours;

    @Schema(description = "培训结果")
    private String result;

    @Schema(description = "证书名称")
    private String certificateName;

    @Schema(description = "证书地址")
    private String certificateUrl;

    @Schema(description = "材料名称")
    private String materialName;

    @Schema(description = "材料地址")
    private String materialUrl;

    @Schema(description = "证书到期日期")
    private LocalDate certificateExpireDate;

    @Schema(description = "下次复训日期")
    private LocalDate retrainDate;

    @Schema(description = "复训提前提醒天数")
    private Integer retrainReminderDays;

    @Schema(description = "关联考试ID")
    private Long examId;

    @Schema(description = "关联问卷ID")
    private Long questionnaireId;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源ID")
    private Long sourceId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "培训评价分数（1-5）")
    private Integer evaluationScore;

    @Schema(description = "培训评价反馈")
    private String evaluationFeedback;

    @Schema(description = "评价时间")
    private LocalDateTime evaluatedTime;
}
