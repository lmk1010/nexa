package com.kyx.service.business.controller.admin.tenant.vo.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 租户功能配置保存 Request VO")
@Data
public class TenantFeatureConfigSaveReqVO {

    @Schema(description = "功能编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "hr.exam.publish")
    @NotBlank(message = "功能编码不能为空")
    private String featureCode;

    @Schema(description = "是否开启跨租户能力", example = "true")
    private Boolean crossTenantEnabled;

}
