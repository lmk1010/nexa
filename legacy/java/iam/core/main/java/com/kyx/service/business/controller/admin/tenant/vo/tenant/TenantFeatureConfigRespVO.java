package com.kyx.service.business.controller.admin.tenant.vo.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 租户功能配置 Response VO")
@Data
public class TenantFeatureConfigRespVO {

    @Schema(description = "功能编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "hr.exam.publish")
    private String featureCode;

    @Schema(description = "是否开启跨租户能力", example = "true")
    private Boolean crossTenantEnabled;

}
