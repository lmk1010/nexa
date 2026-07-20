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
 * 员工打卡记录 DO
 *
 * @author MK
 */
@TableName("hr_attendance_clock_record")
@KeySequence("hr_attendance_clock_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceClockRecordDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 员工档案 ID
     */
    private Long profileId;

    /**
     * 考勤日期
     */
    private LocalDate attendanceDate;

    /**
     * 打卡类型：IN/OUT
     */
    private String clockType;

    /**
     * 打卡时间
     */
    private LocalDateTime clockTime;

    /**
     * 打卡状态：NORMAL/LATE/EARLY
     */
    private String clockStatus;

    /**
     * 数据来源：MANUAL/DINGTALK
     */
    private String sourceType;

    /**
     * 来源记录ID（钉钉流水）
     */
    private String sourceRecordId;

    /**
     * 打卡地点名称
     */
    private String locationName;

    /**
     * 打卡地点地址
     */
    private String locationAddress;

    /**
     * 打卡设备信息
     */
    private String deviceInfo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 原始 payload
     */
    private String rawPayload;

}
