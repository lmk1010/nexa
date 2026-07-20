package com.kyx.service.erp.controller.admin.asset.vo.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - ERP 用户搜索 Request VO")
@Data
public class ErpAssetTransferUserSearchReqVO {

    @Schema(description = "搜索关键词", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    @Schema(description = "部门ID", example = "1")
    private Long deptId;

    @Schema(description = "排除的用户ID", example = "1")
    private Long excludeUserId;

} 