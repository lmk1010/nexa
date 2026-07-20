package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 问卷选项保存 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷选项保存 VO")
@Data
public class QuestionnaireOptionSaveReqVO {

    @Schema(description = "选项内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 1000, message = "选项内容长度不能超过1000个字符")
    private String optionText;

    @Schema(description = "选项分值")
    private Integer optionScore;

    @Schema(description = "选项序号")
    private Integer sortNo;

}
