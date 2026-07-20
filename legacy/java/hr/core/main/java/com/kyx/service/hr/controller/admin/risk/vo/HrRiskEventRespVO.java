package com.kyx.service.hr.controller.admin.risk.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class HrRiskEventRespVO {

    private Long id;

    private String sourceType;

    private String sourceKey;

    private String issueType;

    private String severity;

    private String title;

    private String description;

    private String action;

    private Long profileId;

    private String profileName;

    private String mobile;

    private Long ownerUserId;

    private String ownerName;

    private String routePath;

    private LocalDateTime dueTime;

    private String status;

    private Boolean generatedFlag;

    private LocalDateTime firstSeenTime;

    private LocalDateTime lastSeenTime;

    private LocalDateTime handledTime;

    private Long handledBy;

    private String handledByName;

    private String handleResult;

    private String remark;

    private Date createTime;

}
