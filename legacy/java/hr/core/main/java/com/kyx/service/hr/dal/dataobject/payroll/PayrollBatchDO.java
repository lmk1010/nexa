package com.kyx.service.hr.dal.dataobject.payroll;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Payroll batch.
 */
@TableName("hr_payroll_batch")
@KeySequence("hr_payroll_batch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollBatchDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String payrollMonth;

    private String batchName;

    /**
     * DRAFT / PENDING_APPROVAL / PUBLISHED / LOCKED / REJECTED / CANCELLED.
     */
    private String status;

    private String processInstanceId;

    /**
     * BPM status: 1 running, 2 approved, 3 rejected, 4 cancelled.
     */
    private Integer approvalStatus;

    private LocalDateTime generatedTime;

    private LocalDateTime publishedTime;

    private Long publishedBy;

    private LocalDateTime lockedTime;

    private Long lockedBy;

    private String summaryJson;

}
