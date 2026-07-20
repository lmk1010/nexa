package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 问卷题目保存 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷题目保存 VO")
@Data
public class QuestionnaireItemSaveReqVO {

    @Schema(description = "题目标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 1000, message = "题目标题长度不能超过1000个字符")
    private String title;

    @Schema(description = "题型(single/multi/score/text/score_text/blank)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemType;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "最大分")
    private Integer maxScore;

    @Schema(description = "题目序号")
    private Integer sortNo;

    @Schema(description = "选项列表")
    @Valid
    private List<QuestionnaireOptionSaveReqVO> options;

}
