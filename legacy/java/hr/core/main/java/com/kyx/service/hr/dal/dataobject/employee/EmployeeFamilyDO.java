package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工家庭信息表
 *
 * @author MK
 */
@TableName("hr_employee_family")
@KeySequence("hr_employee_family_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeFamilyDO extends TenantBaseDO {

    /**
     * ID
     */
    @TableId
    private Long id;
    
    /**
     * 员工档案ID
     */
    private Long profileId;
    
    /**
     * 关系
     */
    private String relation;
    
    /**
     * 姓名
     */
    private String name;
    
    /**
     * 性别（1男 2女）
     */
    private Integer gender;
    
    /**
     * 出生日期
     */
    private LocalDate birthDate;
    
    /**
     * 联系电话
     */
    private String phone;
    
    /**
     * 工作单位
     */
    private String workplace;
    
} 