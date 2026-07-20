package com.kyx.service.hr.dal.dataobject.attendance;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 考勤月度结算 DO
 */
@TableName("hr_attendance_monthly_settlement")
@KeySequence("hr_attendance_monthly_settlement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceMonthlySettlementDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String settlementMonth;

    private Long deptId;

    private String status;

    private LocalDateTime generatedTime;

    private LocalDateTime lockedTime;

    private Long lockedBy;

    private String summaryJson;

}
