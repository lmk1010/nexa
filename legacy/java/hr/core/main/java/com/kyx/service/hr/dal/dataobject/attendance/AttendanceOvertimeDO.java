package com.kyx.service.hr.dal.dataobject.attendance;

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
 * Attendance overtime application.
 */
@TableName("hr_attendance_overtime")
@KeySequence("hr_attendance_overtime_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceOvertimeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private Long userId;

    private LocalDate overtimeDate;

    private String overtimeType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal durationHours;

    private Boolean convertToLeave;

    private String leaveTypeCode;

    private BigDecimal balanceHours;

    private Boolean balanceSynced;

    private String reason;

    private String status;

    private String processInstanceId;

    private Long approverId;

    private LocalDateTime approvedTime;

    private String approveRemark;

}
