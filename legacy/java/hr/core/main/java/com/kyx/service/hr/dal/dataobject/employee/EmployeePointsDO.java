package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 员工积分记录表
 *
 * @author MK
 */
@TableName("hr_employee_points")
@KeySequence("hr_employee_points_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePointsDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 员工档案ID
     */
    private Long profileId;

    /**
     * 积分类型：1奖励 2扣减 3兑换 4过期
     */
    private Integer pointsType;

    /**
     * 积分数量（正数加分，负数扣分）
     */
    private BigDecimal points;

    /**
     * 变动后余额
     */
    private BigDecimal balance;

    /**
     * 积分原因
     */
    private String reason;

    /**
     * 来源类型：attendance/performance/activity/manual
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
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

}
