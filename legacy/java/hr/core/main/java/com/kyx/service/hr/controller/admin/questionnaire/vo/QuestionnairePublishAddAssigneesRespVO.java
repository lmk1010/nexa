package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 问卷发布追加填写人 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷发布追加填写人 Response VO")
@Data
public class QuestionnairePublishAddAssigneesRespVO {

    @Schema(description = "发布ID")
    private Long publishId;

    @Schema(description = "批次号")
    private Integer batchNo;

    @Schema(description = "请求人数")
    private Integer requestedCount;

    @Schema(description = "新增任务数")
    private Integer createdCount;

    @Schema(description = "已存在跳过数")
    private Integer skippedDuplicateCount;

    @Schema(description = "无效人员数")
    private Integer invalidUserCount;

}
