package com.kyx.service.hr.controller.admin.joblevel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 职级管理选项 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 职级管理选项 VO")
@Data
public class JobLevelOptionVO {

    @Schema(description = "职级ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long value;

    @Schema(description = "职级名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String label;

} 