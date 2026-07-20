package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工档案表
 *
 * @author MK
 */
@TableName("hr_employee_profile")
@KeySequence("hr_employee_profile_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeProfileDO extends TenantBaseDO {

    /**
     * 员工档案ID
     */
    @TableId
    private Long id;
    
    /**
     * 档案编号
     */
    private String profileNo;
    
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
     * 户籍所在地
     */
    private String hometown;
    
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
    
    /**
     * 手机号码
     */
    private String mobile;
    
    /**
     * 邮箱
     */
    private String email;

    /**
     * 入职时间
     */
    private LocalDate onboardDate;

    /**
     * 岗位职级
     */
    private String jobLevel;

    /**
     * 司龄（单位：年）
     */
    private java.math.BigDecimal companyYears;

    /**
     * 与紧急联系人关系
     */
    private String emergencyRelation;

    /**
     * 转正日期
     */
    private LocalDate confirmationDate;

    /**
     * 父亲生日
     */
    private String fatherBirthday;

    /**
     * 母亲生日
     */
    private String motherBirthday;

    /**
     * 档案状态（1正常 0停用）
     */
    private Integer status;

} 