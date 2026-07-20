package com.kyx.service.hr.controller.admin.exam.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

/**
 * 试卷保存 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 试卷保存 VO")
@Data
public class ExamPaperSaveReqVO {

    @Schema(description = "试卷名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "规则JSON")
    private String ruleJson;

    @Schema(description = "题目列表")
    @Valid
    private List<ExamPaperItemSaveReqVO> items;

}
