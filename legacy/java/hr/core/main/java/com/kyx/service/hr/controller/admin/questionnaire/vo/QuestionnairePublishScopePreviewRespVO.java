package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 问卷发布范围预览 Response VO")
@Data
public class QuestionnairePublishScopePreviewRespVO {

    @Schema(description = "评价人列表")
    private List<QuestionnairePublishScopeUserRespVO> evaluatorUsers = new ArrayList<>();

    @Schema(description = "被评人列表")
    private List<QuestionnairePublishScopeUserRespVO> targetUsers = new ArrayList<>();

    @Schema(description = "预计生成任务数")
    private Integer assignmentCount = 0;
}
