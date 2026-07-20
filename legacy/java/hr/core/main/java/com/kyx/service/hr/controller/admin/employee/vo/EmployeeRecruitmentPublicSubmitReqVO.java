package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Schema(description = "公开招聘 - 候选人投递 Request VO")
@Data
public class EmployeeRecruitmentPublicSubmitReqVO {

    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "投递链接不能为空")
    private String token;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "姓名不能为空")
    private String name;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "期望薪资")
    private BigDecimal expectedSalary;

    @Schema(description = "简历地址")
    private String resumeUrl;

    @Schema(description = "投递来源")
    private String source;

    @Schema(description = "人才标签，逗号分隔")
    private String talentTags;

    @Schema(description = "内推人")
    private String referrerName;

    @Schema(description = "内推人手机号")
    private String referrerMobile;

    @Schema(description = "备注")
    private String remark;
}
