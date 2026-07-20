package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Schema(description = "Admin - Work Requirement Response VO")
@Data
public class WorkRequirementRespVO {

    private Long id;
    private Long parentId;
    private Long rootId;
    private Integer level;
    private String path;
    private Integer childCount;
    private String parentTitle;
    private String title;
    private String description;
    private Integer priority;
    private Integer status;
    private String processInstanceId;
    private Integer approvalStatus;
    private String proposerDept;
    private String targetDept;
    private String proposerName;
    private Long proposerUserId;
    private Long assigneeUserId;
    private String assigneeName;
    private List<WorkRequirementDeveloperRespVO> developerMembers;
    private List<Long> collaboratorUserIds;
    private String collaboratorNames;
    private Date expectedFinishDate;
    private Integer estimatedUserCount;
    private Date submitTestTime;
    private Date testPassTime;
    private Date acceptedTime;
    private Date closeTime;
    private Integer previousStatus;
    private String lastRejectReason;
    private BigDecimal integral;
    private String useType;
    private String sourceIp;
    private List<String> attachmentUrls;
    private Integer commentCount;
    private Integer commentUnreadCount;
    private Date createTime;
    private Date updateTime;

}
