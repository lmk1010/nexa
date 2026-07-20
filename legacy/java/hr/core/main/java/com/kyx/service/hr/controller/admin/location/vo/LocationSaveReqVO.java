package com.kyx.service.hr.controller.admin.location.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 公司地点管理保存 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 公司地点管理保存 Request VO")
@Data
public class LocationSaveReqVO {

    @Schema(description = "地点ID")
    private Long id;

    @Schema(description = "地点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "地点名称不能为空")
    @Size(max = 50, message = "地点名称长度不能超过50个字符")
    private String locationName;

    @Schema(description = "描述")
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @Schema(description = "省份")
    @Size(max = 50, message = "省份长度不能超过50个字符")
    private String province;

    @Schema(description = "市")
    @Size(max = 50, message = "市长度不能超过50个字符")
    private String city;

    @Schema(description = "县/区")
    @Size(max = 50, message = "县/区长度不能超过50个字符")
    private String district;

} 