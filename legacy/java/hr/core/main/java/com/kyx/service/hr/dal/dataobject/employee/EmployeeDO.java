package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工花名册表
 *
 * @author MK
 */
@TableName("hr_employee")
@KeySequence("hr_employee_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeDO extends TenantBaseDO {

    /**
     * 员工ID
     */
    @TableId
    private Long id;
    
    /**
     * 员工编号
     */
    private String employeeNo;
    
    /**
     * 员工档案ID
     */
    private Long profileId;
    
    /**
     * 员工姓名
     */
    private String name;
    
    /**
     * 用户ID（关联系统用户表）
     */
    private Long userId;
    
    /**
     * 身份证号
     */
    private String idNumber;
    
    /**
     * 性别（1男 2女）
     */
    private Integer gender;
    
    /**
     * 出生日期
     */
    private LocalDate birthDate;
    
    /**
     * 年龄
     */
    private Integer age;
    
    /**
     * 手机号码
     */
    private String mobile;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 部门ID
     */
    private Long deptId;
    
    /**
     * 职位
     */
    private String jobTitle;
    
    /**
     * 职级ID
     */
    private Long jobLevelId;
    
    /**
     * 入职日期
     */
    private LocalDate entryDate;
    
    /**
     * 工作状态（1待入职 2试用期 3在职 4离职）
     */
    private Integer workStatus;
    
    /**
     * 用工类型（1全职 2兼职 3劳务 4实习）
     */
    private Integer employmentType;
    
    /**
     * 工作地点ID
     */
    private Long workLocationId;
    
    /**
     * 直属上级ID
     */
    private Long directSupervisorId;
    
    /**
     * 员工状态（1正常 0停用）
     */
    private Integer status;
    
    /**
     * 国籍
     */
    private String nationality;
    
    /**
     * 民族
     */
    private String ethnicity;
    
    /**
     * 政治面貌
     */
    private String politicalStatus;
    
    /**
     * 婚姻状况（1未婚 2已婚 3离异 4丧偶）
     */
    private Integer maritalStatus;
    
    /**
     * 现住址
     */
    private String address;
    
    /**
     * 紧急联系人
     */
    private String emergencyContact;
    
    /**
     * 紧急联系电话
     */
    private String emergencyPhone;
    
} 