package com.kyx.service.hr.controller.admin.exam.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 考试分页 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 考试分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExamPageReqVO extends PageParam {

    @Schema(description = "考试名称")
    private String name;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "考试类型(0一次性 1定期考核)")
    private Integer examType;

    @Schema(description = "绉熸埛ID")
    private Long tenantId;

    @Schema(hidden = true)
    private String creator;

}
