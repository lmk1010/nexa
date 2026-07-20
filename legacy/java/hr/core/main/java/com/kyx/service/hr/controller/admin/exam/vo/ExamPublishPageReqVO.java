package com.kyx.service.hr.controller.admin.exam.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 考试发布分页 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 考试发布分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExamPublishPageReqVO extends PageParam {

    @Schema(description = "考试模板ID")
    private Long examId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "父发布ID(查批次列表用)")
    private Long parentPublishId;

    @Schema(description = "仅看我可参加的发布")
    private Boolean mine;

    @Schema(description = "绉熸埛ID")
    private Long tenantId;

    @Schema(hidden = true)
    private Long currentUserId;

    @Schema(hidden = true)
    private String creator;

}
