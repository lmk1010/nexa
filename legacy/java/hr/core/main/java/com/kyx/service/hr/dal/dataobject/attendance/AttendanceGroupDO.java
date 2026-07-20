package com.kyx.service.hr.dal.dataobject.attendance;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考勤组 DO
 */
@TableName("hr_attendance_group")
@KeySequence("hr_attendance_group_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceGroupDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String groupName;

    private String scopeType;

    private String scopeJson;

    private Long shiftRuleId;

    private Integer status;

    private String remark;

}
