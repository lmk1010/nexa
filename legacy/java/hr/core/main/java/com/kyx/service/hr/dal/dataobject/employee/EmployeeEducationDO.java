package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工教育信息表
 *
 * @author MK
 */
@TableName("hr_employee_education")
@KeySequence("hr_employee_education_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeEducationDO extends TenantBaseDO {

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
     * 学历
     */
    private String education;
    
    /**
     * 学校名称
     */
    private String schoolName;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 入学时间
     */
    private LocalDate enrollmentDate;
    
    /**
     * 毕业时间
     */
    private LocalDate graduationDate;
    
    /**
     * 是否最高学历
     */
    private Boolean isHighest;
    
} 