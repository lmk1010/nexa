package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 员工操作日志表
 *
 * @author MK
 */
@TableName("hr_employee_operation_log")
@KeySequence("hr_employee_operation_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeOperationLogDO extends TenantBaseDO {

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
     * 操作类型：create/update/delete/import/export
     */
    private String operationType;

    /**
     * 操作模块：basic_info/work_info/education/contract等
     */
    private String operationModule;

    /**
     * 操作标题
     */
    private String operationTitle;

    /**
     * 操作内容描述
     */
    private String operationContent;

    /**
     * 变更前数据(JSON)
     */
    private String beforeData;

    /**
     * 变更后数据(JSON)
     */
    private String afterData;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作IP
     */
    private String operationIp;

    /**
     * 操作来源：web/app/api/import
     */
    private String operationSource;

}
