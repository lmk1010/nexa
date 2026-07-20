package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 招聘公开投递链接
 */
@TableName("hr_recruitment_public_link")
@KeySequence("hr_recruitment_public_link_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeRecruitmentPublicLinkDO extends TenantBaseDO {

    @TableId
    private Long id;

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 链接标题
     */
    private String title;

    /**
     * 招聘活动
     */
    private String campaignName;

    /**
     * 招聘需求编号
     */
    private String demandCode;

    /**
     * 职位
     */
    private String position;

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
     * 招聘原因/岗位说明
     */
    private String demandReason;

    /**
     * 招聘渠道
     */
    private String channel;

    /**
     * 招聘来源
     */
    private String source;

    /**
     * 招聘负责人
     */
    private String recruiter;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 是否启用 0-禁用 1-启用
     */
    private Integer enabled;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 最大提交数，0 表示不限
     */
    private Integer maxSubmit;

    /**
     * 已提交数
     */
    private Integer submitCount;

    /**
     * 备注
     */
    private String remark;
}
