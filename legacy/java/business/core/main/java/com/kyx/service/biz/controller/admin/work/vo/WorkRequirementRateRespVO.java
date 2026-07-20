package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "Admin - Work Requirement rate Response VO")
@Data
public class WorkRequirementRateRespVO {

    private Long id;
    private Long requirementId;
    private Integer score;
    private String content;
    private Long raterUserId;
    private String raterName;
    private Long targetUserId;
    private String targetUserName;
    private Date createTime;
    private Date updateTime;

}
