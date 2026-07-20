package com.kyx.service.hr.controller.admin.sequence.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 序列管理分页查询 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 序列管理分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SequencePageReqVO extends PageParam {

    @Schema(description = "序列名称")
    private String sequenceName;

    @Schema(description = "上级序列ID")
    private Long parentId;

    @Schema(description = "状态")
    private Integer status;

} 