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
 * 员工业绩信息表
 *
 * @author MK
 */
@TableName("hr_employee_performance")
@KeySequence("hr_employee_performance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePerformanceDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 员工档案ID
     */
    private Long profileId;

    /**
     * 绩效方案ID
     */
    private Long schemeId;

    /**
     * 绩效方案编码
     */
    private String schemeCode;

    /**
     * 绩效方案名称
     */
    private String schemeName;

    /**
     * 绩效方案类型
     */
    private String schemeType;

    /**
     * 绩效方案周期类型
     */
    private String cycleType;

    /**
     * 考核周期
     */
    private String period;

    /**
     * 考核得分
     */
    private BigDecimal score;

    /**
     * 考核等级
     */
    private String grade;

    /**
     * 考核结果
     */
    private String result;

    /**
     * 考核日期
     */
    private LocalDate evaluatedDate;

    /**
     * 绩效周期状态
     */
    private String cycleStatus;

    /**
     * 绩效目标
     */
    private String goalContent;

    /**
     * 员工自评
     */
    private String selfReview;

    /**
     * 主管评价
     */
    private String managerReview;

    /**
     * 校准结果
     */
    private String calibrationResult;

    /**
     * 面谈时间
     */
    private LocalDateTime interviewTime;

    /**
     * 下一步跟进时间
     */
    private LocalDateTime nextFollowTime;

    /**
     * 绩效结果应用类型
     */
    private String applicationType;

    /**
     * 绩效结果应用状态
     */
    private String applicationStatus;

    /**
     * 绩效结果应用时间
     */
    private LocalDateTime applicationTime;

    /**
     * 绩效结果应用备注
     */
    private String applicationRemark;

    /**
     * 绩效审批状态
     */
    private String approvalStatus;

    /**
     * BPM流程实例ID
     */
    private String processInstanceId;

    /**
     * 绩效提交审批时间
     */
    private LocalDateTime submittedTime;

    /**
     * 绩效审批人
     */
    private Long approvedBy;

    /**
     * 绩效审批时间
     */
    private LocalDateTime approvedTime;

    /**
     * 绩效审批备注
     */
    private String approvalRemark;

    /**
     * 备注
     */
    private String remark;
}
