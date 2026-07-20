package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 员工积分账户表
 *
 * @author MK
 */
@TableName("hr_employee_points_account")
@KeySequence("hr_employee_points_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePointsAccountDO extends TenantBaseDO {

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
     * 累计获得积分
     */
    private BigDecimal totalPoints;

    /**
     * 已使用积分
     */
    private BigDecimal usedPoints;

    /**
     * 已过期积分
     */
    private BigDecimal expiredPoints;

    /**
     * 当前余额
     */
    private BigDecimal balance;

}
