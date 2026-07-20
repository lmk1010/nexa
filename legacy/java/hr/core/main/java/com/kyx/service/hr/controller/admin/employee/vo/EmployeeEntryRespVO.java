package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 员工入职记录 Response VO")
@Data
public class EmployeeEntryRespVO {

    @Schema(description = "入职记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "入职编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String entryNo;

    @Schema(description = "员工档案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long profileId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "员工编号")
    private String employeeNo;

    @Schema(description = "入职类型（1首次入职 2再入职）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer entryType;

    @Schema(description = "入职流程类型（1简易入职 2审批入职）")
    private Integer processType;

    @Schema(description = "入职日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate entryDate;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "岗位编号数组")
    private String postIds;

    @Schema(description = "职位")
    private String jobTitle;

    @Schema(description = "分机号")
    private String extension;

    @Schema(description = "办公地点")
    private String officeLocation;

    @Schema(description = "工作地点ID")
    private Long workLocationId;

    @Schema(description = "直属上级ID")
    private Long directSupervisorId;

    @Schema(description = "直属上级")
    private String directSupervisorName;

    @Schema(description = "用工类型（1全职 2兼职 3劳务 4实习）")
    private Integer employmentType;

    @Schema(description = "试用期月数")
    private Integer probationMonths;

    @Schema(description = "合同类型（1劳动合同 2劳务合同 3实习协议）")
    private Integer contractType;

    @Schema(description = "合同开始日期")
    private LocalDate contractStartDate;

    @Schema(description = "合同结束日期")
    private LocalDate contractEndDate;

    @Schema(description = "工作状态（0待填写 1待入职 2试用期 3在职 4离职）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer workStatus;

    @Schema(description = "入职状态（1待提交 2审批中 3已通过 4已拒绝 5已取消）")
    private Integer onboardingStatus;

    @Schema(description = "BPM流程实例ID")
    private String processInstanceId;

    @Schema(description = "离职日期")
    private LocalDate leaveDate;

    @Schema(description = "离职原因")
    private String leaveReason;

    @Schema(description = "取消入职原因")
    private String cancelReason;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "银行分支")
    private String bankBranch;

    @Schema(description = "银行账户")
    private String bankAccount;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ========== 员工档案基本信息 ==========
    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别（1男 2女）")
    private Integer gender;

    @Schema(description = "身份证号")
    private String idNumber;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "国籍")
    private String nationality;

    @Schema(description = "民族")
    private String ethnicity;

    @Schema(description = "政治面貌")
    private String politicalStatus;

    @Schema(description = "婚姻状况（1未婚 2已婚 3离异 4丧偶）")
    private Integer maritalStatus;

    @Schema(description = "户籍所在地")
    private String hometown;

    @Schema(description = "现住址")
    private String address;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    private String emergencyPhone;

    // ========== 教育信息 ==========
    @Schema(description = "教育信息列表")
    private List<EducationInfo> educationList;

    // ========== 家庭信息 ==========
    @Schema(description = "家庭信息列表")
    private List<FamilyInfo> familyList;

    @Schema(description = "教育信息")
    @Data
    public static class EducationInfo {
        @Schema(description = "ID")
        private Long id;

        @Schema(description = "学历")
        private String education;

        @Schema(description = "学校名称")
        private String schoolName;

        @Schema(description = "专业")
        private String major;

        @Schema(description = "入学时间")
        private LocalDate enrollmentDate;

        @Schema(description = "毕业时间")
        private LocalDate graduationDate;

        @Schema(description = "是否最高学历")
        private Boolean isHighest;
    }

    @Schema(description = "家庭信息")
    @Data
    public static class FamilyInfo {
        @Schema(description = "ID")
        private Long id;

        @Schema(description = "关系")
        private String relation;

        @Schema(description = "姓名")
        private String name;

        @Schema(description = "性别（1男 2女）")
        private Integer gender;

        @Schema(description = "出生日期")
        private LocalDate birthDate;

        @Schema(description = "联系电话")
        private String phone;

        @Schema(description = "工作单位")
        private String workplace;
    }
} 
