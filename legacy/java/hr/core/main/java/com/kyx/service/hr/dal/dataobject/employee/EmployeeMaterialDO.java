package com.kyx.service.hr.dal.dataobject.employee;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Employee electronic material ledger.
 */
@TableName("hr_employee_material")
@KeySequence("hr_employee_material_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeMaterialDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private Long userId;

    private String category;

    private String materialType;

    private String materialName;

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private LocalDate issueDate;

    private LocalDate expireDate;

    private String status;

    private LocalDateTime submittedTime;

    private Long reviewerId;

    private LocalDateTime reviewedTime;

    private String rejectReason;

    private String sourceType;

    private Long sourceId;

    private String remark;

}
