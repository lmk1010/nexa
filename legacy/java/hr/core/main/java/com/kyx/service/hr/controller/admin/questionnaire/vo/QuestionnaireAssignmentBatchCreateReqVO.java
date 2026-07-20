package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

/**
 * 问卷分配批量创建 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷分配批量创建 VO")
@Data
public class QuestionnaireAssignmentBatchCreateReqVO {

    @Schema(description = "分配列表")
    @Valid
    private List<QuestionnaireAssignmentSaveReqVO> assignments;

}
