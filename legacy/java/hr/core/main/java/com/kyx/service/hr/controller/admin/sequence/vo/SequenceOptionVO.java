package com.kyx.service.hr.controller.admin.sequence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 序列选项 VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 序列选项 VO")
@Data
public class SequenceOptionVO {

    @Schema(description = "序列ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long value;

    @Schema(description = "序列名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String label;

} 