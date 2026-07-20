package com.kyx.service.hr.dal.dataobject.administrative;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 假期余额 DO
 */
@TableName("hr_leave_balance")
@KeySequence("hr_leave_balance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrLeaveBalanceDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private Long userId;

    private Long leaveTypeId;

    private String leaveTypeCode;

    private Integer year;

    private BigDecimal totalAmount;

    private BigDecimal usedAmount;

    private BigDecimal frozenAmount;

    private BigDecimal remainAmount;

    private String remark;

}
