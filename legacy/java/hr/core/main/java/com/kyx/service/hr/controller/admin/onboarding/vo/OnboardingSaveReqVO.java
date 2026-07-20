package com.kyx.service.hr.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 入职申请创建/修改 Request VO")
@Data
public class OnboardingSaveReqVO {

    @Schema(description = "入职申请ID")
    private Long id;

    @Schema(description = "申请人姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "申请人姓名不能为空")
    @Size(max = 30, message = "申请人姓名长度不能超过 30 个字符")
    private String applicantName;

    @Schema(description = "申请人手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "申请人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String applicantMobile;

    @Schema(description = "申请人邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    private String applicantEmail;

    @Schema(description = "申请人身份证号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "申请人身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$", message = "身份证号格式不正确")
    private String applicantIdCard;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "性别不能为空")
    @Min(value = 1, message = "性别值不正确")
    @Max(value = 2, message = "性别值不正确")
    private Integer gender;

    @Schema(description = "出生日期")
    private LocalDate birthday;

    @Schema(description = "国籍")
    @Size(max = 50, message = "国籍长度不能超过 50 个字符")
    private String nationality;

    @Schema(description = "民族")
    @Size(max = 50, message = "民族长度不能超过 50 个字符")
    private String ethnicity;

    @Schema(description = "政治面貌")
    @Size(max = 50, message = "政治面貌长度不能超过 50 个字符")
    private String politicalStatus;

    @Schema(description = "婚姻状况")
    @Min(value = 1, message = "婚姻状况值不正确")
    @Max(value = 4, message = "婚姻状况值不正确")
    private Integer maritalStatus;

    @Schema(description = "籍贯")
    @Size(max = 200, message = "籍贯长度不能超过 200 个字符")
    private String nativePlace;

    @Schema(description = "现住址")
    @Size(max = 500, message = "现住址长度不能超过 500 个字符")
    private String currentAddress;

    @Schema(description = "户籍地址")
    @Size(max = 500, message = "户籍地址长度不能超过 500 个字符")
    private String permanentAddress;

    @Schema(description = "紧急联系人")
    @Size(max = 30, message = "紧急联系人长度不能超过 30 个字符")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    @Size(max = 20, message = "紧急联系电话长度不能超过 20 个字符")
    private String emergencyPhone;

    @Schema(description = "学历")
    @Min(value = 1, message = "学历值不正确")
    @Max(value = 8, message = "学历值不正确")
    private Integer educationLevel;

    @Schema(description = "毕业院校")
    @Size(max = 200, message = "毕业院校长度不能超过 200 个字符")
    private String schoolName;

    @Schema(description = "专业")
    @Size(max = 100, message = "专业长度不能超过 100 个字符")
    private String major;

    @Schema(description = "毕业日期")
    private LocalDate graduationDate;

    @Schema(description = "工作经历")
    private String workExperience;

    @Schema(description = "期望职位")
    @Size(max = 100, message = "期望职位长度不能超过 100 个字符")
    private String expectedPosition;

    @Schema(description = "期望薪资")
    @DecimalMin(value = "0.01", message = "期望薪资必须大于0")
    private BigDecimal expectedSalary;

    @Schema(description = "期望入职日期")
    private LocalDate expectedEntryDate;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "岗位编号数组")
    @Size(max = 255, message = "岗位编号数组长度不能超过 255 个字符")
    private String postIds;

    @Schema(description = "用工类型")
    @Min(value = 1, message = "用工类型值不正确")
    @Max(value = 4, message = "用工类型值不正确")
    private Integer employmentType;

    @Schema(description = "试用期月数")
    @Min(value = 0, message = "试用期月数不能小于0")
    @Max(value = 12, message = "试用期月数不能超过12")
    private Integer probationMonths;

    @Schema(description = "合同类型")
    @Min(value = 1, message = "合同类型值不正确")
    @Max(value = 3, message = "合同类型值不正确")
    private Integer contractType;

    @Schema(description = "合同期限类型")
    @Min(value = 1, message = "合同期限类型值不正确")
    @Max(value = 3, message = "合同期限类型值不正确")
    private Integer durationType;

    @Schema(description = "合同开始日期")
    private LocalDate contractStartDate;

    @Schema(description = "合同结束日期")
    private LocalDate contractEndDate;

    @Schema(description = "银行名称")
    @Size(max = 100, message = "银行名称长度不能超过 100 个字符")
    private String bankName;

    @Schema(description = "开户行")
    @Size(max = 200, message = "开户行长度不能超过 200 个字符")
    private String bankBranch;

    @Schema(description = "银行账号")
    @Size(max = 50, message = "银行账号长度不能超过 50 个字符")
    private String bankAccountNo;

    @Schema(description = "账户名")
    @Size(max = 50, message = "账户名长度不能超过 50 个字符")
    private String bankAccountName;

    @Schema(description = "审批类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审批类型不能为空")
    @Min(value = 1, message = "审批类型值不正确")
    @Max(value = 2, message = "审批类型值不正确")
    private Integer approvalType;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

} 