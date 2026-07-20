package com.kyx.service.hr.controller.admin.sequence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 序列管理保存 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 序列管理保存 Request VO")
@Data
public class SequenceSaveReqVO {

    @Schema(description = "序列ID")
    private Long id;

    @Schema(description = "序列名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "序列名称不能为空")
    @Size(max = 50, message = "序列名称长度不能超过50个字符")
    private String sequenceName;

    @Schema(description = "序列描述")
    @Size(max = 200, message = "序列描述长度不能超过200个字符")
    private String description;

    @Schema(description = "上级序列ID")
    private Long parentId;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

} 