package com.kyx.service.hr.controller.admin.selfservice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - Employee quick action config Response VO")
@Data
public class HrQuickActionConfigRespVO {

    private Long id;

    private String actionCode;

    private String actionName;

    private String icon;

    private String routePath;

    private String category;

    private String permissionCode;

    private Integer sortOrder;

    private Integer status;

}
