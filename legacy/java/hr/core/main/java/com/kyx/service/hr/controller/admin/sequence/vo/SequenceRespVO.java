package com.kyx.service.hr.controller.admin.sequence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 序列管理 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 序列管理 Response VO")
@Data
public class SequenceRespVO {

    @Schema(description = "序列ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "序列名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sequenceName;

    @Schema(description = "序列描述")
    private String description;

    @Schema(description = "上级序列ID")
    private Long parentId;

    @Schema(description = "上级序列名称")
    private String parentName;

    @Schema(description = "序列层级")
    private Integer level;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

    @Schema(description = "子序列列表")
    private List<SequenceRespVO> children;

} 