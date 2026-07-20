package com.kyx.service.hr.controller.admin.location.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 公司地点管理分页 Request VO
 *
 * @author MK
 */
@Schema(description = "管理后台 - 公司地点管理分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LocationPageReqVO extends PageParam {

    @Schema(description = "地点名称")
    private String locationName;

} 