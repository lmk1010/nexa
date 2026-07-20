package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 员工考勤月度统计表
 *
 * @author MK
 */
@TableName("hr_employee_attendance_stat")
@KeySequence("hr_employee_attendance_stat_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeAttendanceStatDO extends TenantBaseDO {

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
     * 统计年份
     */
    private Integer statYear;

    /**
     * 统计月份
     */
    private Integer statMonth;

    /**
     * 应出勤天数
     */
    private Integer workDays;

    /**
     * 实际出勤天数
     */
    private Integer actualDays;

    /**
     * 迟到次数
     */
    private Integer lateCount;

    /**
     * 早退次数
     */
    private Integer earlyLeaveCount;

    /**
     * 缺勤次数
     */
    private Integer absentCount;

    /**
     * 缺卡次数
     */
    private Integer missingClockCount;

    /**
     * 加班时长(小时)
     */
    private BigDecimal overtimeHours;

    /**
     * 请假天数
     */
    private BigDecimal leaveDays;

    /**
     * 出差天数
     */
    private BigDecimal tripDays;

}
