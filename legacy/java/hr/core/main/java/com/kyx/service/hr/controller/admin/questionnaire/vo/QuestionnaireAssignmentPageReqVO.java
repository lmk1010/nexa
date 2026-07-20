package com.kyx.service.hr.controller.admin.questionnaire.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 问卷分配分页 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 问卷分配分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QuestionnaireAssignmentPageReqVO extends PageParam {

    @Schema(description = "问卷ID")
    private Long questionnaireId;

    @Schema(description = "发布ID")
    private Long publishId;

    @Schema(description = "批次号")
    private Integer batchNo;

    @Schema(description = "评价人ID")
    private Long evaluatorId;

    @Schema(description = "评价人姓名")
    private String evaluatorName;

    @Schema(description = "被评人ID")
    private Long targetId;

    @Schema(description = "被评人姓名")
    private String targetName;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "是否仅查询当前可见任务")
    private Boolean visibleOnly;

}
