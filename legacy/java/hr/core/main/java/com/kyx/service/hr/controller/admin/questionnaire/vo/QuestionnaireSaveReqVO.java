package com.kyx.service.hr.controller.admin.questionnaire.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 问卷保存 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷保存 Request VO")
@Data
public class QuestionnaireSaveReqVO {

    @Schema(description = "问卷ID")
    private Long id;

    @Schema(description = "问卷编码，为空时后端自动生成")
    @Size(max = 64, message = "问卷编码长度不能超过64个字符")
    private String code;

    @Schema(description = "问卷名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问卷名称不能为空")
    @Size(max = 200, message = "问卷名称长度不能超过200个字符")
    private String name;

    @Schema(description = "问卷类型(peer/employee_impression/exam)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "问卷类型不能为空")
    private String type;

    @Schema(description = "状态(0草稿 1已发布 2已关闭)")
    private Integer status;

    @Schema(description = "适用角色(manager/employee/both)")
    private String roleScope;

    @Schema(description = "被评对象规则(JSON)")
    private String targetRuleJson;

    @Schema(description = "周期开始")
    private LocalDate periodStart;

    @Schema(description = "周期结束")
    private LocalDate periodEnd;

    @Schema(description = "题目列表")
    @Valid
    private List<QuestionnaireItemSaveReqVO> items;

}
