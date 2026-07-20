package com.kyx.service.business.controller.admin.work.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Admin - Work notice response")
@Data
public class WorkNoticeRespVO {

    @Schema(description = "Notice id", example = "1024")
    private Long id;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Summary")
    private String summary;

    @Schema(description = "Redirect URL")
    private String redirectUrl;

    @Schema(description = "Type", example = "system")
    private String type;

    @Schema(description = "Sender")
    private String sender;

    @Schema(description = "Read status", example = "false")
    private Boolean readStatus;

    @Schema(description = "Need confirmation", example = "true")
    private Boolean needConfirm;

    @Schema(description = "Confirmed", example = "false")
    private Boolean confirmed;

    @Schema(description = "Confirm time")
    private LocalDateTime confirmTime;

    @Schema(description = "Confirm deadline")
    private LocalDateTime confirmDeadline;

    @Schema(description = "Confirm total count", example = "20")
    private Integer confirmTotalCount;

    @Schema(description = "Confirmed count", example = "16")
    private Integer confirmedCount;

    @Schema(description = "Unconfirmed count", example = "4")
    private Integer unconfirmedCount;

    @Schema(description = "Overdue count", example = "2")
    private Integer overdueCount;

    @Schema(description = "Created time")
    private LocalDateTime createTime;
}
