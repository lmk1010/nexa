package com.kyx.service.hr.api.onboarding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "RPC 服务 - 公共入职申请 Request DTO")
@Data
public class OnboardingPublicReqDTO {

    @Schema(description = "入职记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "入职记录ID不能为空")
    private Long entryId;

    @Schema(description = "审批类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "审批类型不能为空")
    private Integer approvalType;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "入职流程类型（1简易入职 2审批入职）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "入职流程类型不能为空")
    private Integer processType;

    // 基本信息
    @Schema(description = "员工姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "员工姓名")
    @NotBlank(message = "员工姓名不能为空")
    private String employeeName;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "employee@example.com")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "性别不能为空")
    private Integer gender;

    // 个人信息
    @Schema(description = "身份证号", requiredMode = Schema.RequiredMode.REQUIRED, example = "110101199001011234")
    @NotBlank(message = "身份证号不能为空")
    private String idNumber;

    @Schema(description = "出生日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "1990-01-01")
    @NotNull(message = "出生日期不能为空")
    private LocalDate birthDate;

    @Schema(description = "年龄", requiredMode = Schema.RequiredMode.REQUIRED, example = "34")
    @NotNull(message = "年龄不能为空")
    private Integer age;

    @Schema(description = "国籍", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国")
    @NotBlank(message = "国籍不能为空")
    private String nationality;

    @Schema(description = "民族", requiredMode = Schema.RequiredMode.REQUIRED, example = "汉族")
    @NotBlank(message = "民族不能为空")
    private String ethnicity;

    @Schema(description = "政治面貌", requiredMode = Schema.RequiredMode.REQUIRED, example = "群众")
    @NotBlank(message = "政治面貌不能为空")
    private String politicalStatus;

    @Schema(description = "婚姻状况", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "婚姻状况不能为空")
    private Integer maritalStatus;

    @Schema(description = "户籍所在地", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区")
    @NotBlank(message = "户籍所在地不能为空")
    private String hometown;

    @Schema(description = "现住址", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京市朝阳区建国路88号")
    @NotBlank(message = "现住址不能为空")
    private String address;

    @Schema(description = "紧急联系人", requiredMode = Schema.RequiredMode.REQUIRED, example = "紧急联系人")
    @NotBlank(message = "紧急联系人不能为空")
    private String emergencyContact;

    @Schema(description = "紧急联系电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13900139000")
    @NotBlank(message = "紧急联系电话不能为空")
    private String emergencyPhone;

    // 教育情况
    @Schema(description = "教育经历列表")
    private List<EducationInfo> educationList;

    // 工资卡信息
    @Schema(description = "银行名称", example = "中国工商银行")
    private String bankName;

    @Schema(description = "银行分支", example = "中国工商银行北京分行")
    private String bankBranch;

    @Schema(description = "银行账户", example = "6222021234567890123")
    private String bankAccount;

    @Schema(description = "账户名称", example = "员工姓名")
    private String accountName;

    // 家庭信息
    @Schema(description = "家庭成员列表")
    private List<FamilyInfo> familyList;

    // 附件信息
    @Schema(description = "身份证正面", example = "/uploads/idcard/front.jpg")
    private String idCardFront;

    @Schema(description = "身份证背面", example = "/uploads/idcard/back.jpg")
    private String idCardBack;

    @Schema(description = "教育证书", example = "/uploads/education/cert.jpg")
    private String educationCert;

    @Schema(description = "资格证书", example = "/uploads/qualification/cert.jpg")
    private String qualificationCert;

    @Schema(description = "其他文件", example = "/uploads/other/doc.pdf")
    private String otherDocuments;

    @Schema(description = "附件列表")
    private List<AttachmentInfo> attachmentList;

    @Schema(description = "教育信息")
    @Data
    public static class EducationInfo {
        @Schema(description = "学历", example = "本科")
        private String education;

        @Schema(description = "学校名称", example = "北京大学")
        private String schoolName;

        @Schema(description = "专业", example = "计算机科学与技术")
        private String major;

        @Schema(description = "入学时间", example = "2008-09-01")
        private LocalDate enrollmentDate;

        @Schema(description = "毕业时间", example = "2012-07-01")
        private LocalDate graduationDate;

        @Schema(description = "是否最高学历", example = "true")
        private Boolean isHighest;
    }

    @Schema(description = "家庭信息")
    @Data
    public static class FamilyInfo {
        @Schema(description = "关系", example = "父亲")
        private String relation;

        @Schema(description = "姓名", example = "李父")
        private String name;

        @Schema(description = "联系电话", example = "13900139001")
        private String phone;

        @Schema(description = "工作单位", example = "退休")
        private String workplace;
    }

    @Schema(description = "附件信息")
    @Data
    public static class AttachmentInfo {
        @Schema(description = "附件类型", example = "身份证")
        private String attachmentType;

        @Schema(description = "附件名称", example = "身份证正面")
        private String attachmentName;

        @Schema(description = "文件地址", example = "/uploads/attachment.jpg")
        private String fileUrl;

        @Schema(description = "文件大小", example = "1024")
        private Long fileSize;

        @Schema(description = "文件类型", example = "image/jpeg")
        private String fileType;

        @Schema(description = "备注", example = "清晰可见")
        private String remark;
    }

}
