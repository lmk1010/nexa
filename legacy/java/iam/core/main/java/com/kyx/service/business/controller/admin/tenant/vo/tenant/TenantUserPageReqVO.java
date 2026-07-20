package com.kyx.service.business.controller.admin.tenant.vo.tenant;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 租户用户分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TenantUserPageReqVO extends PageParam {

    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long tenantId;

    @Schema(description = "用户名", example = "user001")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "手机号", example = "13800138000")
    private String mobile;

}
