package com.kyx.service.hr.controller.admin.joblevel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 职级管理 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 职级管理 Response VO")
@Data
public class JobLevelRespVO {

    @Schema(description = "职级ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "职级名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String levelName;

    @Schema(description = "职级编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String levelCode;

    @Schema(description = "职级描述")
    private String description;

    @Schema(description = "所属序列ID")
    private Long sequenceId;

    @Schema(description = "所属序列名称")
    private String sequenceName;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户名称")
    private String tenantName;

} 