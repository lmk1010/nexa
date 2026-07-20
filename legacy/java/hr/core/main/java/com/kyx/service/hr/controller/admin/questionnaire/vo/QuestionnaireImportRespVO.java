package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 问卷导入 Response VO")
@Data
public class QuestionnaireImportRespVO {

    @Schema(description = "问卷名称")
    private String name;

    @Schema(description = "问卷类型(peer/employee_impression/exam)")
    private String type;

    @Schema(description = "题目列表")
    private List<QuestionnaireItemSaveReqVO> items;

}
