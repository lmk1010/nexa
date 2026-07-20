package com.kyx.service.business.controller.admin.notice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 通知公告信息 Response VO")
@Data
public class NoticeRespVO {

    @Schema(description = "通知公告序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "小博主")
    private String title;

    @Schema(description = "公告类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "小博主")
    private Integer type;

    @Schema(description = "公告内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "半生编码")
    private String content;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "接收范围：ALL 全员，USER 指定人员", example = "USER")
    private String receiverType;

    @Schema(description = "接收用户编号列表")
    private List<Long> receiverUserIds;

    @Schema(description = "是否需要确认", example = "true")
    private Boolean needConfirm;

    @Schema(description = "确认截止时间")
    private LocalDateTime confirmDeadline;

    @Schema(description = "应确认人数", example = "20")
    private Integer confirmTotalCount;

    @Schema(description = "已确认人数", example = "16")
    private Integer confirmedCount;

    @Schema(description = "未确认人数", example = "4")
    private Integer unconfirmedCount;

    @Schema(description = "逾期未确认人数", example = "2")
    private Integer overdueCount;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "时间戳格式")
    private LocalDateTime createTime;

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "租户名称", example = "集团租户")
    private String tenantName;

}
