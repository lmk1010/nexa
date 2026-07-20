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
 * 员工培训信息表
 *
 * @author MK
 */
@TableName("hr_employee_training")
@KeySequence("hr_employee_training_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeTrainingDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 员工档案ID
     */
    private Long profileId;

    /**
     * 关联课程ID
     */
    private Long courseId;

    /**
     * 关联学习计划ID
     */
    private Long planId;

    /**
     * 关联学习任务ID
     */
    private Long assignmentId;

    /**
     * 培训名称
     */
    private String trainingName;

    /**
     * 培训机构
     */
    private String provider;

    /**
     * 培训开始日期
     */
    private LocalDate startDate;

    /**
     * 培训结束日期
     */
    private LocalDate endDate;

    /**
     * 培训时长（小时）
     */
    private BigDecimal hours;

    /**
     * 培训结果
     */
    private String result;

    /**
     * 证书名称
     */
    private String certificateName;

    /**
     * 证书地址
     */
    private String certificateUrl;

    /**
     * 材料名称
     */
    private String materialName;

    /**
     * 材料地址
     */
    private String materialUrl;

    /**
     * 证书到期日期
     */
    private LocalDate certificateExpireDate;

    /**
     * 下次复训日期
     */
    private LocalDate retrainDate;

    /**
     * 复训提前提醒天数
     */
    private Integer retrainReminderDays;

    /**
     * 关联考试ID
     */
    private Long examId;

    /**
     * 关联问卷ID
     */
    private Long questionnaireId;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 来源ID
     */
    private Long sourceId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 培训评价分数（1-5）
     */
    private Integer evaluationScore;

    /**
     * 培训评价反馈
     */
    private String evaluationFeedback;

    /**
     * 评价时间
     */
    private LocalDateTime evaluatedTime;
}
