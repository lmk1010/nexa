package com.kyx.service.hr.dal.dataobject.onboarding;

import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入职申请表
 *
 * @author MK
 */
@TableName("hr_onboarding")
@KeySequence("hr_onboarding_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class OnboardingDO extends TenantBaseDO {

    /**
     * 入职申请ID
     */
    @TableId
    private Long id;
    
    /**
     * 申请编号
     */
    private String applicationNo;
    
    /**
     * 申请人姓名
     */
    private String applicantName;
    
    /**
     * 申请人手机号
     */
    private String applicantMobile;
    
    /**
     * 申请人邮箱
     */
    private String applicantEmail;
    
    /**
     * 申请人身份证号
     */
    private String applicantIdCard;
    
    /**
     * 性别（1男 2女）
     */
    private Integer gender;
    
    /**
     * 出生日期
     */
    private LocalDate birthday;
    
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
     * 籍贯
     */
    private String nativePlace;
    
    /**
     * 现住址
     */
    private String currentAddress;
    
    /**
     * 户籍地址
     */
    private String permanentAddress;
    
    /**
     * 紧急联系人
     */
    private String emergencyContact;
    
    /**
     * 紧急联系电话
     */
    private String emergencyPhone;
    
    /**
     * 学历（1小学 2初中 3高中 4中专 5大专 6本科 7硕士 8博士）
     */
    private Integer educationLevel;
    
    /**
     * 毕业院校
     */
    private String schoolName;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 毕业日期
     */
    private LocalDate graduationDate;
    
    /**
     * 工作经历
     */
    private String workExperience;
    
    /**
     * 期望职位
     */
    private String expectedPosition;
    
    /**
     * 期望薪资
     */
    private BigDecimal expectedSalary;
    
    /**
     * 期望入职日期
     */
    private LocalDate expectedEntryDate;
    
    /**
     * 部门ID
     */
    private Long deptId;
    
    /**
     * 岗位编号数组
     */
    private String postIds;
    
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
     * 合同期限类型（1固定期限 2无固定期限 3以完成工作为期限）
     */
    private Integer durationType;
    
    /**
     * 合同开始日期
     */
    private LocalDate contractStartDate;
    
    /**
     * 合同结束日期
     */
    private LocalDate contractEndDate;
    
    /**
     * 银行名称
     */
    private String bankName;
    
    /**
     * 开户行
     */
    private String bankBranch;
    
    /**
     * 银行账号
     */
    private String bankAccountNo;
    
    /**
     * 账户名
     */
    private String bankAccountName;
    
    /**
     * 审批类型（1简易审批 2BPM流程审批）
     */
    private Integer approvalType;
    
    /**
     * 状态（1待提交 2审批中 3已通过 4已拒绝 5已取消）
     */
    private Integer status;
    
    /**
     * 审批意见
     */
    private String approvalComment;
    
    /**
     * 审批人ID
     */
    private Long approverId;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;
    
    /**
     * BPM流程ID
     */
    private String bpmProcessId;
    
    /**
     * BPM任务ID
     */
    private String bpmTaskId;
    
    /**
     * 员工ID（审批通过后生成）
     */
    private Long employeeId;
    
    /**
     * 员工编号（审批通过后生成）
     */
    private String employeeNo;
    
    /**
     * 备注
     */
    private String remark;

} 