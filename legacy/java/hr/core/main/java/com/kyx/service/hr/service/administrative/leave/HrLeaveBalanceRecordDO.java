package com.kyx.service.hr.service.administrative.leave;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@TableName("hr_leave_balance_record")
@KeySequence("hr_leave_balance_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class HrLeaveBalanceRecordDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long balanceId;

    private Long profileId;

    private Long userId;

    private Long leaveTypeId;

    private String leaveTypeCode;

    private Integer year;

    private String changeType;

    private BigDecimal changeAmount;

    private BigDecimal beforeTotalAmount;

    private BigDecimal afterTotalAmount;

    private BigDecimal beforeUsedAmount;

    private BigDecimal afterUsedAmount;

    private BigDecimal beforeFrozenAmount;

    private BigDecimal afterFrozenAmount;

    private BigDecimal beforeRemainAmount;

    private BigDecimal afterRemainAmount;

    private Long operatorId;

    private String sourceType;

    private Long sourceId;

    private String remark;

}
