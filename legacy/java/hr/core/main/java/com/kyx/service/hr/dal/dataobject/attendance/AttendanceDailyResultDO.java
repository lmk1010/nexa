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
 * 考勤每日结果 DO
 */
@TableName("hr_attendance_daily_result")
@KeySequence("hr_attendance_daily_result_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceDailyResultDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private Long userId;

    private LocalDate attendanceDate;

    private LocalDateTime expectedStartTime;

    private LocalDateTime expectedEndTime;

    private LocalDateTime actualStartTime;

    private LocalDateTime actualEndTime;

    private String resultStatus;

    private Integer lateMinutes;

    private Integer earlyLeaveMinutes;

    private BigDecimal absentHours;

    private BigDecimal leaveHours;

    private BigDecimal tripHours;

    private String sourceJson;

    private LocalDateTime calculatedTime;

}
