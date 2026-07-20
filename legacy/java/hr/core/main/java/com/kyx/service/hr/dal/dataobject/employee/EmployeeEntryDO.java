package com.kyx.service.hr.dal.dataobject.employee;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工入职记录表
 *
 * @author MK
 */
@TableName("hr_employee_entry")
@KeySequence("hr_employee_entry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeEntryDO extends TenantBaseDO {

    /**
     * 入职记录ID
     */
    @TableId
    private Long id;
    
    /**
     * 入职编号
     */
    private String entryNo;
    
    /**
     * 员工档案ID
     */
    private Long profileId;
    
    /**
     * 用户ID（关联系统用户表）
     */
    private Long userId;
    
    /**
     * 员工编号（正式入职后生成）
     */
    private String employeeNo;
    
    /**
     * 入职类型（1首次入职 2再入职）
     */
    private Integer entryType;
    
    /**
     * 入职流程类型（1简易入职 2审批入职）
     */
    private Integer processType;
    
    /**
     * 入职日期
     */
    private LocalDate entryDate;
    
    /**
     * 部门ID
     */
    private Long deptId;
    
    /**
     * 岗位编号数组
     */
    private String postIds;
    
    /**
     * 职位
     */
    private String jobTitle;

    /**
     * 分机号
     */
    private String extension;

    /**
     * 办公地点
     */
    private String officeLocation;
    
    /**
     * 职级ID
     */
    private Long jobLevelId;
    
    /**
     * 序列ID
     */
    private Long jobSequenceId;
    
    /**
     * 工作地点ID
     */
    private Long workLocationId;

    /**
     * 直属上级ID
     */
    private Long directSupervisorId;
    
    /**
     * 用工类型（1全职 2兼职 3劳务 4实习）
     */
    private Integer employmentType;
    
    /**
     * 试用期月数
     */
    private Integer probationMonths;
    
    /**
     * 合同类型（1劳动合同 2劳务合同 3实习协议）
     */
    private Integer contractType;
    
    /**
     * 合同开始日期
     */
    private LocalDate contractStartDate;
    
    /**
     * 合同结束日期
     */
    private LocalDate contractEndDate;
    
    /**
     * 工作状态（0待填写 1待入职 2试用期 3在职 4离职）
     */
    private Integer workStatus;
    
    /**
     * 入职状态（1待提交 2审批中 3已通过 4已拒绝 5已取消）
     */
    private Integer onboardingStatus;

    /**
     * BPM流程实例ID
     */
    private String processInstanceId;

    /**
     * 离职日期
     */
    private LocalDate leaveDate;
    
    /**
     * 离职原因
     */
    private String leaveReason;
    
    /**
     * 取消入职原因
     */
    private String cancelReason;
    
    /**
     * 银行名称
     */
    private String bankName;
    
    /**
     * 银行分支
     */
    private String bankBranch;
    
    /**
     * 银行账户
     */
    private String bankAccount;
    
    /**
     * 账户名称
     */
    private String accountName;
    
    /**
     * 备注
     */
    private String remark;
    
} 
