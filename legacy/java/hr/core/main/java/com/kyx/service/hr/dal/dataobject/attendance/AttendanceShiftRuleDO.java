package com.kyx.service.hr.dal.dataobject.attendance;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 考勤班次规则 DO
 */
@TableName("hr_attendance_shift_rule")
@KeySequence("hr_attendance_shift_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceShiftRuleDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String shiftName;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalTime restStartTime;

    private LocalTime restEndTime;

    private Integer lateGraceMinutes;

    private Integer earlyLeaveGraceMinutes;

    private BigDecimal workHours;

    private Boolean defaultFlag;

    private Integer status;

    private String remark;

}
