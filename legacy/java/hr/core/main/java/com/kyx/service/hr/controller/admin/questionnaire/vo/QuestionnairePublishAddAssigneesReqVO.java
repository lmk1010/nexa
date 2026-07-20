package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 问卷发布追加填写人 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷发布追加填写人 Request VO")
@Data
public class QuestionnairePublishAddAssigneesReqVO {

    @Schema(description = "发布ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发布ID不能为空")
    private Long publishId;

    @Schema(description = "批次号，为空则使用当前批次")
    private Integer batchNo;

    @Schema(description = "新增填写人/评价人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择新增填写人")
    private List<Long> evaluatorUserIds;

    @Schema(description = "互评问卷被评人ID")
    private List<Long> targetUserIds;

    @Schema(description = "是否通知新增填写人")
    private Boolean sendNotice;

}
