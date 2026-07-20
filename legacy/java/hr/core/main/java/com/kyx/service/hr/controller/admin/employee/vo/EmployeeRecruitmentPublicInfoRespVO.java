package com.kyx.service.hr.controller.admin.employee.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "公开招聘 - 投递页信息 Response VO")
@Data
public class EmployeeRecruitmentPublicInfoRespVO {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "链接标题")
    private String title;

    @Schema(description = "招聘活动")
    private String campaignName;

    @Schema(description = "招聘需求编号")
    private String demandCode;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "用人部门")
    private String demandDeptName;

    @Schema(description = "招聘 HC")
    private Integer demandHeadcount;

    @Schema(description = "招聘预算")
    private BigDecimal demandBudget;

    @Schema(description = "招聘原因/岗位说明")
    private String demandReason;

    @Schema(description = "招聘渠道")
    private String channel;

    @Schema(description = "招聘来源")
    private String source;

    @Schema(description = "招聘负责人")
    private String recruiter;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime expireTime;

    @Schema(description = "最大提交数")
    private Integer maxSubmit;

    @Schema(description = "已提交数")
    private Integer submitCount;
}
