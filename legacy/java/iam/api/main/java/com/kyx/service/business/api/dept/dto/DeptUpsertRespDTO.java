package com.kyx.service.business.api.dept.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPC 服务 - 部门同步创建或更新 Response DTO")
@Data
public class DeptUpsertRespDTO {

    @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1084734607")
    private Long id;

    @Schema(description = "是否新建")
    private Boolean created;

    @Schema(description = "是否更新")
    private Boolean updated;
}
