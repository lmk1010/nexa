package com.kyx.service.hr.controller.admin.questionnaire.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 问卷发布分页 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷发布分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnairePublishPageReqVO extends PageParam {

    @Schema(description = "问卷ID")
    private Long questionnaireId;

    @Schema(description = "状态(0未发布 1已发布 2已截止)")
    private Integer status;

}
