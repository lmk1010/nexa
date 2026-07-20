package com.kyx.service.finance.dal.dataobject.closing;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 期间锁定记录 DO
 */
@TableName("finance_period_lock")
@KeySequence("finance_period_lock_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinancePeriodLockDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long companyId;

    private String period;

    private String lockStatus;

    private String lockReason;

    private String lockedBy;

    private LocalDateTime lockedTime;

    private String unlockedBy;

    private LocalDateTime unlockedTime;
}

