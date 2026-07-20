package com.kyx.service.hr.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 入职申请 Response VO")
@Data
public class OnboardingRespVO {

    @Schema(description = "入职申请ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "申请编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String applicationNo;

    @Schema(description = "申请人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String applicantName;

    @Schema(description = "申请人手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String applicantMobile;

    @Schema(description = "申请人邮箱")
    private String applicantEmail;

    @Schema(description = "申请人身份证号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String applicantIdCard;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer gender;

    @Schema(description = "出生日期")
    private LocalDate birthday;

    @Schema(description = "国籍")
    private String nationality;

    @Schema(description = "民族")
    private String ethnicity;

    @Schema(description = "政治面貌")
    private String politicalStatus;

    @Schema(description = "婚姻状况")
    private Integer maritalStatus;

    @Schema(description = "籍贯")
    private String nativePlace;

    @Schema(description = "现住址")
    private String currentAddress;

    @Schema(description = "户籍地址")
    private String permanentAddress;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    private String emergencyPhone;

    @Schema(description = "学历")
    private Integer educationLevel;

    @Schema(description = "毕业院校")
    private String schoolName;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "毕业日期")
    private LocalDate graduationDate;

    @Schema(description = "工作经历")
    private String workExperience;

    @Schema(description = "期望职位")
    private String expectedPosition;

    @Schema(description = "期望薪资")
    private BigDecimal expectedSalary;

    @Schema(description = "期望入职日期")
    private LocalDate expectedEntryDate;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "岗位编号数组")
    private String postIds;

    @Schema(description = "用工类型")
    private Integer employmentType;

    @Schema(description = "试用期月数")
    private Integer probationMonths;

    @Schema(description = "合同类型")
    private Integer contractType;

    @Schema(description = "合同期限类型")
    private Integer durationType;

    @Schema(description = "合同开始日期")
    private LocalDate contractStartDate;

    @Schema(description = "合同结束日期")
    private LocalDate contractEndDate;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "开户行")
    private String bankBranch;

    @Schema(description = "银行账号")
    private String bankAccountNo;

    @Schema(description = "账户名")
    private String bankAccountName;

    @Schema(description = "审批类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer approvalType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "审批意见")
    private String approvalComment;

    @Schema(description = "审批人ID")
    private Long approverId;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "BPM流程ID")
    private String bpmProcessId;

    @Schema(description = "BPM任务ID")
    private String bpmTaskId;

    @Schema(description = "员工ID")
    private Long employeeId;

    @Schema(description = "员工编号")
    private String employeeNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

} 