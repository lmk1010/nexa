package com.kyx.service.biz.controller.admin.hotel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "Admin - Hotel work order create Request VO")
@Data
public class HotelWorkOrderSaveReqVO {
    private Long id;
    @NotBlank
    private String store;
    private String roomNo;
    @NotBlank
    private String title;
    @NotBlank
    private String type;
    @NotBlank
    private String priority;
    @NotBlank
    private String content;
    private String source;
    private Long sourceRecordId;
    private String sourceRecordTitle;
    private String customerEmotion;
    @NotNull
    private Long assigneeUserId;
    private String assigneeName;
    private String assigneeImUserId;
}
