package com.kyx.service.hr.controller.admin.employee.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EmployeeMaterialRespVO {

    private Long id;

    private Long profileId;

    private String profileName;

    private Long userId;

    private String userNickname;

    private String category;

    private String materialType;

    private String materialName;

    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private LocalDate issueDate;

    private LocalDate expireDate;

    private Long expireDays;

    private String status;

    private LocalDateTime submittedTime;

    private Long reviewerId;

    private String reviewerName;

    private LocalDateTime reviewedTime;

    private String rejectReason;

    private String sourceType;

    private Long sourceId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
