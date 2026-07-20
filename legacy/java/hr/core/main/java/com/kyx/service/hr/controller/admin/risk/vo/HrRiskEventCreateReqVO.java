package com.kyx.service.hr.controller.admin.risk.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class HrRiskEventCreateReqVO {

    @NotBlank(message = "风险标题不能为空")
    private String title;

    @NotBlank(message = "风险等级不能为空")
    private String severity;

    private String issueType;

    private String description;

    private String action;

    private Long profileId;

    private Long ownerUserId;

    private String routePath;

    private LocalDateTime dueTime;

    private String remark;

}
