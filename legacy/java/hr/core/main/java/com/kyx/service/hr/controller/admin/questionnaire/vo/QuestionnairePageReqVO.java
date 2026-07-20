package com.kyx.service.hr.controller.admin.questionnaire.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 问卷分页 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnairePageReqVO extends PageParam {

    @Schema(description = "问卷名称")
    private String name;

    @Schema(description = "问卷类型(peer/employee_impression/exam)")
    private String type;

    @Schema(description = "状态(0草稿 1已发布 2已关闭)")
    private Integer status;

}
