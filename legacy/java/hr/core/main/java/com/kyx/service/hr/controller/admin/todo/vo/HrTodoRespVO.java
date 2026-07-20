package com.kyx.service.hr.controller.admin.todo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - HR todo Response VO")
@Data
public class HrTodoRespVO {

    private Long id;

    private Long assigneeUserId;

    private String assigneeName;

    private Long profileId;

    private String profileName;

    private String businessType;

    private Long businessId;

    private String taskType;

    private String title;

    private String content;

    private String routePath;

    private String status;

    private String priority;

    private LocalDateTime dueTime;

    private Boolean generatedFlag;

    private LocalDateTime completedTime;

    private Long completedBy;

    private String completedByName;

    private String cancelReason;

    private LocalDateTime createTime;

}
