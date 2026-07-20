package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 试卷题目保存 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 试卷题目保存 VO")
@Data
public class ExamPaperItemSaveReqVO {

    @Schema(description = "题目标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "题型(single/multi/judge/blank/short)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String itemType;

    @Schema(description = "选项JSON")
    private String optionsJson;

    @Schema(description = "答案JSON")
    private String answerJson;

    @Schema(description = "分值")
    private Integer score;

    @Schema(description = "是否必答")
    private Boolean required;

    @Schema(description = "题目序号")
    private Integer sortNo;

}
