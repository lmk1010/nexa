package com.kyx.service.hr.dal.dataobject.attendance;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Monthly attendance confirmation by employee.
 */
@TableName("hr_attendance_monthly_confirm")
@KeySequence("hr_attendance_monthly_confirm_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceMonthlyConfirmDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long settlementId;

    private String settlementMonth;

    private Long deptId;

    private Long profileId;

    private Long userId;

    /**
     * PENDING / CONFIRMED / ISSUE / RESOLVED.
     */
    private String status;

    private LocalDateTime confirmedTime;

    private LocalDateTime issueTime;

    private String issueRemark;

    private LocalDateTime resolvedTime;

    private Long resolvedBy;

    private String resolveRemark;

}
