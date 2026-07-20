package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 问卷分配保存 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷分配保存 VO")
@Data
public class QuestionnaireAssignmentSaveReqVO {

    @Schema(description = "分配ID")
    private Long id;

    @Schema(description = "问卷ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "问卷ID不能为空")
    private Long questionnaireId;

    @Schema(description = "发布ID")
    private Long publishId;

    @Schema(description = "评价人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "评价人ID不能为空")
    private Long evaluatorId;

    @Schema(description = "评价人姓名")
    private String evaluatorName;

    @Schema(description = "被评人ID，互评问卷必填")
    private Long targetId;

    @Schema(description = "被评人姓名")
    private String targetName;

    @Schema(description = "角色(manager/employee)")
    private String role;

    @Schema(description = "状态(0待填 1已提交)")
    private Integer status;

}
