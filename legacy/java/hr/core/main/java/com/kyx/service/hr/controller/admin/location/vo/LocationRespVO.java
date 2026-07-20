package com.kyx.service.hr.controller.admin.location.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公司地点管理 Response VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 公司地点管理 Response VO")
@Data
public class LocationRespVO {

    @Schema(description = "地点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "地点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String locationName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "县/区")
    private String district;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 