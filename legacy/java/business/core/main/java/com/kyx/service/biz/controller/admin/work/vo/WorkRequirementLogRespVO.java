package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "Admin - Work Requirement log Response VO")
@Data
public class WorkRequirementLogRespVO {

    private Long id;
    private Long requirementId;
    private String actionType;
    private Integer fromStatus;
    private Integer toStatus;
    private String remark;
    private Long operatorUserId;
    private String operatorName;
    private Date createTime;

}
