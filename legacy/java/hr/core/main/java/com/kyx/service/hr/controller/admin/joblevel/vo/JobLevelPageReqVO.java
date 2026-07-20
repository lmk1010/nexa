package com.kyx.service.hr.controller.admin.joblevel.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 职级管理分页 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 职级管理分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class JobLevelPageReqVO extends PageParam {

    @Schema(description = "职级名称")
    private String levelName;

    @Schema(description = "职级编码")
    private String levelCode;

    @Schema(description = "所属序列ID")
    private Long sequenceId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "租户ID")
    private Long tenantId;

} 