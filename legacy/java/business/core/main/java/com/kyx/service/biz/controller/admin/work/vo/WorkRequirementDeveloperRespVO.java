package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - Work Requirement developer member Response VO")
@Data
public class WorkRequirementDeveloperRespVO {

    private Long id;
    private Long requirementId;
    private Long userId;
    private String userName;
    private Long userTenantId;
    private String memberRole;

}
