package com.kyx.service.hr.controller.admin.selfservice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "Admin - Employee quick action config save Request VO")
@Data
public class HrQuickActionConfigSaveReqVO {

    private Long id;

    @NotBlank(message = "入口编码不能为空")
    private String actionCode;

    @NotBlank(message = "入口名称不能为空")
    private String actionName;

    @NotBlank(message = "图标不能为空")
    private String icon;

    @NotBlank(message = "路由不能为空")
    private String routePath;

    private String category;

    private String permissionCode;

    private Integer sortOrder;

    private Integer status;

}
