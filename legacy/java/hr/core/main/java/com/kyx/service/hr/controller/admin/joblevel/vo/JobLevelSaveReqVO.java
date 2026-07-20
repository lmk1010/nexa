package com.kyx.service.hr.controller.admin.joblevel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 职级管理保存 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 职级管理保存 Request VO")
@Data
public class JobLevelSaveReqVO {

    @Schema(description = "职级ID")
    private Long id;

    @Schema(description = "职级名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "职级名称不能为空")
    @Size(max = 50, message = "职级名称长度不能超过50个字符")
    private String levelName;

    @Schema(description = "职级编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "职级编码不能为空")
    @Size(max = 20, message = "职级编码长度不能超过20个字符")
    private String levelCode;

    @Schema(description = "职级描述")
    @Size(max = 200, message = "职级描述长度不能超过200个字符")
    private String description;

    @Schema(description = "所属序列ID")
    private Long sequenceId;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

} 