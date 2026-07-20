package com.kyx.service.hr.controller.admin.employee.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 员工招聘信息新增/修改 Request VO")
@Data
public class EmployeeRecruitmentSaveReqVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工档案ID不能为空")
    private Long profileId;

    @Schema(description = "招聘渠道")
    private String channel;

    @Schema(description = "招聘来源")
    private String source;

    @Schema(description = "招聘活动")
    private String campaignName;

    @Schema(description = "内推人")
    private String referrerName;

    @Schema(description = "内推人手机号")
    private String referrerMobile;

    @Schema(description = "渠道成本")
    private BigDecimal channelCost;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "招聘负责人")
    private String recruiter;

    @Schema(description = "招聘需求编号")
    private String demandCode;

    @Schema(description = "用人部门")
    private String demandDeptName;

    @Schema(description = "招聘 HC")
    private Integer demandHeadcount;

    @Schema(description = "招聘预算")
    private BigDecimal demandBudget;

    @Schema(description = "招聘原因")
    private String demandReason;

    @Schema(description = "需求状态")
    private String demandStatus;

    @Schema(description = "招聘需求 BPM 流程实例 ID")
    private String demandProcessInstanceId;

    @Schema(description = "需求审批人")
    private String demandApprover;

    @Schema(description = "候选阶段")
    private String candidateStage;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "期望薪资")
    private BigDecimal expectedSalary;

    @Schema(description = "面试时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime interviewTime;

    @Schema(description = "面试官")
    private String interviewer;

    @Schema(description = "面试结果")
    private String interviewResult;

    @Schema(description = "面试评分")
    private BigDecimal interviewScore;

    @Schema(description = "面试结论")
    private String interviewDecision;

    @Schema(description = "面试评价")
    private String interviewFeedback;

    @Schema(description = "面试评价时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime interviewEvaluationTime;

    @Schema(description = "下次跟进时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime nextFollowTime;

    @Schema(description = "最近联系时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime lastContactTime;

    @Schema(description = "触达状态")
    private String touchStatus;

    @Schema(description = "最近触达时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime touchTime;

    @Schema(description = "触达备注")
    private String touchRemark;

    @Schema(description = "人才状态")
    private String talentStatus;

    @Schema(description = "人才标签，逗号分隔")
    private String talentTags;

    @Schema(description = "简历地址")
    private String resumeUrl;

    @Schema(description = "简历解析状态")
    private String resumeParseStatus;

    @Schema(description = "简历解析时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime resumeParseTime;

    @Schema(description = "简历摘要")
    private String resumeSummary;

    @Schema(description = "简历技能")
    private String resumeSkills;

    @Schema(description = "工作年限")
    private BigDecimal resumeWorkYears;

    @Schema(description = "最高学历")
    private String resumeEducation;

    @Schema(description = "最近公司")
    private String resumeLastCompany;

    @Schema(description = "黑名单原因")
    private String blacklistReason;

    @Schema(description = "Offer 日期")
    private LocalDate offerDate;

    @Schema(description = "Offer 状态")
    private String offerStatus;

    @Schema(description = "Offer BPM 流程实例 ID")
    private String offerProcessInstanceId;

    @Schema(description = "Offer 薪资")
    private BigDecimal offerSalary;

    @Schema(description = "入职日期")
    private LocalDate entryDate;

    @Schema(description = "流失原因")
    private String lossReason;

    @Schema(description = "招聘状态")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
