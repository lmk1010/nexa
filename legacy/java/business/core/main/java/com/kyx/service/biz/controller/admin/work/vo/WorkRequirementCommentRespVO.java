package com.kyx.service.biz.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Schema(description = "Admin - Work Requirement comment Response VO")
@Data
public class WorkRequirementCommentRespVO {

    private Long id;
    private Long requirementId;
    private String commentType;
    private String content;
    private Long fromUserId;
    private String fromUserName;
    private Long targetUserId;
    private String targetUserName;
    private String ip;
    private List<String> attachmentUrls;
    private Boolean readStatus;
    private Date createTime;

}
