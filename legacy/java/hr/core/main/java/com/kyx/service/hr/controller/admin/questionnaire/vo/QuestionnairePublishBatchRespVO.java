package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问卷发布批次 Response VO
 */
@Data
@Schema(description = "管理后台 - 问卷发布批次 Response VO")
public class QuestionnairePublishBatchRespVO {

    @Schema(description = "发布ID")
    private Long publishId;

    @Schema(description = "问卷ID")
    private Long questionnaireId;

    @Schema(description = "批次号")
    private Integer batchNo;

    @Schema(description = "批次标签")
    private String batchLabel;

    @Schema(description = "批次开始时间")
    private LocalDateTime batchStartAt;

    @Schema(description = "批次截止时间")
    private LocalDateTime batchEndAt;

    @Schema(description = "应填人数")
    private Integer totalCount;

    @Schema(description = "已交人数")
    private Integer submittedCount;

    @Schema(description = "未交人数")
    private Integer pendingCount;

    @Schema(description = "未交人员姓名")
    private List<String> pendingUserNames;

    @Schema(description = "状态(1进行中 2已完成 3已截止)")
    private Integer status;
}
