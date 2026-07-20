package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - 员工更新 Request VO")
@Data
public class EmployeeUpdateReqVO {

    @Schema(description = "员工ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工ID不能为空")
    private Long id;

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "身份证号")
    private String idNumber;

    @Schema(description = "性别（1男 2女）")
    private Integer gender;

    @Schema(description = "员工状态（1正常 0停用）")
    private Integer status;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "国籍")
    private String nationality;

    @Schema(description = "民族")
    private String ethnicity;

    @Schema(description = "政治面貌")
    private String politicalStatus;

    @Schema(description = "婚姻状况（1未婚 2已婚 3离异 4丧偶）")
    private Integer maritalStatus;

    @Schema(description = "籍贯")
    private String nativePlace;

    @Schema(description = "现住址")
    private String address;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    private String emergencyPhone;

    @Schema(description = "与紧急联系人关系")
    private String emergencyRelation;

    @Schema(description = "入职时间")
    private LocalDate onboardDate;

    @Schema(description = "转正日期")
    private LocalDate confirmationDate;

    @Schema(description = "父亲生日")
    private String fatherBirthday;

    @Schema(description = "母亲生日")
    private String motherBirthday;
}
