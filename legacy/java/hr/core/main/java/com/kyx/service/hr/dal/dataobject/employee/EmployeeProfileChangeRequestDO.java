package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Employee profile self-service change request.
 */
@TableName("hr_employee_profile_change_request")
@KeySequence("hr_employee_profile_change_request_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeProfileChangeRequestDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private Long userId;

    private String changeType;

    private String beforeJson;

    private String afterJson;

    private String changeSummary;

    private String reason;

    private String status;

    private String processInstanceId;

    private Long approverId;

    private LocalDateTime approvedTime;

    private String approveRemark;

}
