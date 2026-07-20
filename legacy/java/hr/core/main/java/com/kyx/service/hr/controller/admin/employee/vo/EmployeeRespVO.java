package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 员工花名册 Response VO")
@Data
public class EmployeeRespVO {

    @Schema(description = "员工ID")
    private Long id;

    @Schema(description = "入职记录ID")
    private Long entryId;

    @Schema(description = "员工编号")
    private String employeeNo;

    @Schema(description = "员工工号")
    private String profileNo;

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "性别（1男 2女）")
    private Integer gender;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "身份证号")
    private String idNumber;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "职位")
    private String jobTitle;

    @Schema(description = "职级名称")
    private String jobLevelName;

    @Schema(description = "工作状态（1待入职 2试用期 3在职 4离职）")
    private Integer workStatus;

    @Schema(description = "用工类型（1全职 2兼职 3劳务 4实习）")
    private Integer employmentType;

    @Schema(description = "试用期（月）")
    private Integer probationMonths;

    @Schema(description = "合同类型（1劳动合同 2劳务合同 3实习协议）")
    private Integer contractType;

    @Schema(description = "离职日期")
    private LocalDate leaveDate;

    @Schema(description = "离职原因")
    private String leaveReason;

    @Schema(description = "工作地点")
    private String workLocation;

    @Schema(description = "直属上级")
    private String directSupervisor;

    @Schema(description = "直属上级档案ID（hr_employee_profile.id）")
    private Long directSupervisorId;

    @Schema(description = "员工状态（1正常 0停用）")
    private Integer status;

    @Schema(description = "国籍")
    private String nationality;

    @Schema(description = "民族")
    private String ethnicity;

    @Schema(description = "政治面貌")
    private String politicalStatus;

    @Schema(description = "婚姻状况（1未婚 2已婚 3离异 4丧偶）")
    private Integer maritalStatus;

    @Schema(description = "现住址")
    private String address;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    private String emergencyPhone;

    @Schema(description = "与紧急联系人关系")
    private String emergencyRelation;

    @Schema(description = "籍贯")
    private String nativePlace;

    @Schema(description = "最高学历")
    private String education;

    @Schema(description = "毕业院校")
    private String school;

    @Schema(description = "所学专业")
    private String major;

    @Schema(description = "毕业时间")
    private LocalDate graduationDate;

    @Schema(description = "开户行")
    private String bankName;

    @Schema(description = "银行卡号")
    private String bankAccount;

    @Schema(description = "开户支行")
    private String bankBranch;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "分机号")
    private String extension;

    @Schema(description = "办公地点")
    private String officeLocation;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "入职时间")
    private LocalDate onboardDate;

    @Schema(description = "转正日期")
    private LocalDate confirmationDate;

    @Schema(description = "岗位职级")
    private String jobLevel;

    @Schema(description = "司龄（年）")
    private java.math.BigDecimal companyYears;

    @Schema(description = "司龄（格式化）")
    private String companyAge;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "父亲生日")
    private String fatherBirthday;

    @Schema(description = "母亲生日")
    private String motherBirthday;

    @Schema(description = "合同开始日期")
    private LocalDate contractStartDate;

    @Schema(description = "合同结束日期")
    private LocalDate contractEndDate;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户名称")
    private String tenantName;
} 
