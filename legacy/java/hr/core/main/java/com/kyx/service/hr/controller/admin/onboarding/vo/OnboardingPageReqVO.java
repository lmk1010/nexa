package com.kyx.service.hr.controller.admin.onboarding.vo;

import com.kyx.foundation.common.pojo.PageParam;
import com.kyx.foundation.common.validation.InEnum;
import com.kyx.service.hr.enums.ApprovalTypeEnum;
import com.kyx.service.hr.enums.OnboardingStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 入职申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OnboardingPageReqVO extends PageParam {

    @Schema(description = "申请编号")
    private String applicationNo;

    @Schema(description = "申请人姓名")
    private String applicantName;

    @Schema(description = "申请人手机号")
    private String applicantMobile;

    @Schema(description = "申请人身份证号")
    private String applicantIdCard;

    @Schema(description = "性别")
    private Integer gender;

    @Schema(description = "状态")
    @InEnum(value = OnboardingStatusEnum.class, message = "状态必须是 {value}")
    private Integer status;

    @Schema(description = "审批类型")
    @InEnum(value = ApprovalTypeEnum.class, message = "审批类型必须是 {value}")
    private Integer approvalType;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "期望入职日期开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedEntryDateStart;

    @Schema(description = "期望入职日期结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedEntryDateEnd;

    @Schema(description = "创建时间开始")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间结束")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeEnd;

} 