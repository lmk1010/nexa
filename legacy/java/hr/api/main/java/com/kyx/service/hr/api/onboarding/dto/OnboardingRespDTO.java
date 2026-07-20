package com.kyx.service.hr.api.onboarding.dto;

import com.fhs.core.trans.vo.VO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - 入职申请 Response DTO")
@Data
public class OnboardingRespDTO implements VO {

    @Schema(description = "入职申请ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "申请编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ON20250120001")
    private String applicationNo;

    @Schema(description = "申请人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "员工姓名")
    private String applicantName;

    @Schema(description = "申请人手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    private String applicantMobile;

    @Schema(description = "申请人邮箱", example = "employee@example.com")
    private String applicantEmail;

    @Schema(description = "申请人身份证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    private String applicantIdCard;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer gender;

    @Schema(description = "出生日期", example = "1990-01-01")
    private LocalDate birthday;

    @Schema(description = "国籍", example = "中国")
    private String nationality;

    @Schema(description = "民族", example = "汉族")
    private String ethnicity;

    @Schema(description = "政治面貌", example = "群众")
    private String politicalStatus;

    @Schema(description = "婚姻状况", example = "1")
    private Integer maritalStatus;

    @Schema(description = "籍贯", example = "北京市")
    private String nativePlace;

    @Schema(description = "现住址", example = "北京市朝阳区xxx街道xxx号")
    private String currentAddress;

    @Schema(description = "户籍地址", example = "北京市朝阳区xxx街道xxx号")
    private String permanentAddress;

    @Schema(description = "紧急联系人", example = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话", example = "13900139000")
    private String emergencyPhone;

    @Schema(description = "学历", example = "6")
    private Integer educationLevel;

    @Schema(description = "毕业院校", example = "北京大学")
    private String schoolName;

    @Schema(description = "专业", example = "计算机科学与技术")
    private String major;

    @Schema(description = "毕业日期", example = "2012-07-01")
    private LocalDate graduationDate;

    @Schema(description = "工作经历", example = "2012-2024 某公司 软件工程师")
    private String workExperience;

    @Schema(description = "期望职位", example = "高级软件工程师")
    private String expectedPosition;

    @Schema(description = "期望薪资", example = "15000.00")
    private BigDecimal expectedSalary;

    @Schema(description = "期望入职日期", example = "2025-02-01")
    private LocalDate expectedEntryDate;

    @Schema(description = "部门ID", example = "1")
    private Long deptId;

    @Schema(description = "岗位编号数组", example = "1,2,3")
    private String postIds;

    @Schema(description = "用工类型", example = "1")
    private Integer employmentType;

    @Schema(description = "试用期月数", example = "3")
    private Integer probationMonths;

    @Schema(description = "合同类型", example = "1")
    private Integer contractType;

    @Schema(description = "合同期限类型", example = "1")
    private Integer durationType;

    @Schema(description = "合同开始日期", example = "2025-02-01")
    private LocalDate contractStartDate;

    @Schema(description = "合同结束日期", example = "2028-01-31")
    private LocalDate contractEndDate;

    @Schema(description = "银行名称", example = "中国工商银行")
    private String bankName;

    @Schema(description = "开户行", example = "中国工商银行北京分行")
    private String bankBranch;

    @Schema(description = "银行账号", example = "6222021234567890123")
    private String bankAccountNo;

    @Schema(description = "账户名", example = "员工姓名")
    private String bankAccountName;

    @Schema(description = "审批类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer approvalType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "审批意见", example = "同意入职")
    private String approvalComment;

    @Schema(description = "审批人ID", example = "1001")
    private Long approverId;

    @Schema(description = "审批时间", example = "2025-01-20 10:30:00")
    private LocalDateTime approvalTime;

    @Schema(description = "BPM流程ID", example = "proc_001")
    private String bpmProcessId;

    @Schema(description = "BPM任务ID", example = "task_001")
    private String bpmTaskId;

    @Schema(description = "员工ID", example = "2001")
    private Long employeeId;

    @Schema(description = "员工编号", example = "EMP001")
    private String employeeNo;

    @Schema(description = "备注", example = "优秀候选人")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-01-20 09:00:00")
    private LocalDateTime createTime;

}
