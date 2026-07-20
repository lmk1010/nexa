package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 问卷发布范围用户预览 Response VO")
@Data
public class QuestionnairePublishScopeUserRespVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名称")
    private String nickname;

    @Schema(description = "部门ID")
    private Long deptId;
}
