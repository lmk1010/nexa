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
 * Attendance correction / field clock application.
 */
@TableName("hr_attendance_correction")
@KeySequence("hr_attendance_correction_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceCorrectionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private Long userId;

    private LocalDate attendanceDate;

    private String applyType;

    private String clockType;

    private LocalDateTime clockTime;

    private String reason;

    private String locationName;

    private String locationAddress;

    private String attachmentJson;

    private String status;

    private String processInstanceId;

    private Long approverId;

    private LocalDateTime approvedTime;

    private String approveRemark;

    private Long clockRecordId;

    private Long exceptionId;

}
