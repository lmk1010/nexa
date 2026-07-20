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
 * Employee document/proof request.
 */
@TableName("hr_employee_document_request")
@KeySequence("hr_employee_document_request_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeDocumentRequestDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long profileId;

    private Long userId;

    private String requestType;

    private String title;

    private String purpose;

    private LocalDate expectedDate;

    private String deliveryMode;

    private String contactInfo;

    private String attachmentJson;

    private String status;

    private Long handlerId;

    private LocalDateTime handledTime;

    private String handleRemark;

    private String resultFileUrl;

    private String resultFileName;

}
