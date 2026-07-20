package com.kyx.service.hr.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - HR notice record response")
@Data
public class HrNoticeRecordRespVO {

    private Long id;

    private String noticeKey;

    private String channel;

    private String businessType;

    private Long businessId;

    private Long receiverUserId;

    private String receiverName;

    private String title;

    private String content;

    private String sendStatus;

    private LocalDateTime sendTime;

    private String errorMessage;

    private Integer retryCount;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
