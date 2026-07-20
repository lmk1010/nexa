package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工招聘信息表
 *
 * @author MK
 */
@TableName("hr_employee_recruitment")
@KeySequence("hr_employee_recruitment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeRecruitmentDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 员工档案ID
     */
    private Long profileId;

    /**
     * 招聘渠道
     */
    private String channel;

    /**
     * 招聘来源
     */
    private String source;

    /**
     * 招聘活动
     */
    private String campaignName;

    /**
     * 内推人
     */
    private String referrerName;

    /**
     * 内推人手机号
     */
    private String referrerMobile;

    /**
     * 渠道成本
     */
    private BigDecimal channelCost;

    /**
     * 职位
     */
    private String position;

    /**
     * 招聘负责人
     */
    private String recruiter;

    /**
     * 招聘需求编号
     */
    private String demandCode;

    /**
     * 用人部门
     */
    private String demandDeptName;

    /**
     * 招聘 HC
     */
    private Integer demandHeadcount;

    /**
     * 招聘预算
     */
    private BigDecimal demandBudget;

    /**
     * 招聘原因
     */
    private String demandReason;

    /**
     * 需求状态：DRAFT, APPROVING, APPROVED, PAUSED, CLOSED
     */
    private String demandStatus;

    /**
     * 招聘需求 BPM 流程实例 ID
     */
    private String demandProcessInstanceId;

    /**
     * 需求审批人
     */
    private String demandApprover;

    /**
     * 候选阶段
     */
    private String candidateStage;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 期望薪资
     */
    private BigDecimal expectedSalary;

    /**
     * 面试时间
     */
    private LocalDateTime interviewTime;

    /**
     * 面试官
     */
    private String interviewer;

    /**
     * 面试结果
     */
    private String interviewResult;

    /**
     * 面试评分
     */
    private BigDecimal interviewScore;

    /**
     * 面试结论：PASS, PENDING, REJECT
     */
    private String interviewDecision;

    /**
     * 面试评价
     */
    private String interviewFeedback;

    /**
     * 面试评价时间
     */
    private LocalDateTime interviewEvaluationTime;

    /**
     * 下次跟进时间
     */
    private LocalDateTime nextFollowTime;

    /**
     * 最近联系时间
     */
    private LocalDateTime lastContactTime;

    /**
     * 触达状态：PENDING, CONTACTED, RESPONDED, NURTURED
     */
    private String touchStatus;

    /**
     * 最近触达时间
     */
    private LocalDateTime touchTime;

    /**
     * 触达备注
     */
    private String touchRemark;

    /**
     * 人才状态：POOL, INTERVIEWING, OFFERING, HIRED, ELIMINATED, BLACKLISTED
     */
    private String talentStatus;

    /**
     * 人才标签，逗号分隔
     */
    private String talentTags;

    /**
     * 简历地址
     */
    private String resumeUrl;

    /**
     * 简历解析状态：PENDING, PARSED, FAILED
     */
    private String resumeParseStatus;

    /**
     * 简历解析时间
     */
    private LocalDateTime resumeParseTime;

    /**
     * 简历摘要
     */
    private String resumeSummary;

    /**
     * 简历技能
     */
    private String resumeSkills;

    /**
     * 工作年限
     */
    private BigDecimal resumeWorkYears;

    /**
     * 最高学历
     */
    private String resumeEducation;

    /**
     * 最近公司
     */
    private String resumeLastCompany;

    /**
     * 黑名单原因
     */
    private String blacklistReason;

    /**
     * Offer 日期
     */
    private LocalDate offerDate;

    /**
     * Offer 状态
     */
    private String offerStatus;

    /**
     * Offer BPM 流程实例 ID
     */
    private String offerProcessInstanceId;

    /**
     * Offer 薪资
     */
    private BigDecimal offerSalary;

    /**
     * 入职日期
     */
    private LocalDate entryDate;

    /**
     * 流失原因
     */
    private String lossReason;

    /**
     * 招聘状态
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
