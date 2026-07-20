package com.kyx.service.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Admin - Employee document request Response VO")
@Data
public class EmployeeDocumentRequestRespVO {

    private Long id;

    private Long profileId;

    private String profileName;

    private Long userId;

    private String userNickname;

    private String requestType;

    private String title;

    private String purpose;

    private LocalDate expectedDate;

    private String deliveryMode;

    private String contactInfo;

    private String attachmentJson;

    private String status;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime handledTime;

    private String handleRemark;

    private String resultFileUrl;

    private String resultFileName;

    private LocalDateTime createTime;

}
