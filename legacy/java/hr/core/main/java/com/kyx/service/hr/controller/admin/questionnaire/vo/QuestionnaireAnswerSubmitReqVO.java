package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 问卷答案提交 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷答案提交 VO")
@Data
public class QuestionnaireAnswerSubmitReqVO {

    @Schema(description = "分配ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分配ID不能为空")
    private Long assignmentId;

    @Schema(description = "问卷ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "问卷ID不能为空")
    private Long questionnaireId;

    @Schema(description = "答案列表")
    @Valid
    private List<QuestionnaireAnswerItemSaveReqVO> answers;

}
