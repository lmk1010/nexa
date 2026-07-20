package com.kyx.service.business.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 通知确认明细 Response VO")
@Data
public class NoticeConfirmDetailRespVO {

    @Schema(description = "记录编号", example = "1024")
    private Long id;

    @Schema(description = "公告编号", example = "2048")
    private Long noticeId;

    @Schema(description = "用户编号", example = "1")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "用户账号")
    private String userAccount;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "催办次数")
    private Integer remindCount;

    @Schema(description = "最近催办时间")
    private LocalDateTime lastRemindTime;

    @Schema(description = "是否已确认")
    private Boolean confirmed;

}
