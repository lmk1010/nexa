package com.kyx.service.hr.dal.dataobject.attendance;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤异常 DO
 */
@TableName("hr_attendance_exception")
@KeySequence("hr_attendance_exception_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceExceptionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long dailyResultId;

    private Long profileId;

    private Long userId;

    private LocalDate attendanceDate;

    private String exceptionType;

    private String exceptionStatus;

    private String reason;

    private Long handlerId;

    private LocalDateTime handledTime;

    private String handleRemark;

}
