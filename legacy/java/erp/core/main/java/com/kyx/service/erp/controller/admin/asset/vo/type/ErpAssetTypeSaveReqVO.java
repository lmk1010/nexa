package com.kyx.service.erp.controller.admin.asset.vo.type;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - ERP 资产类型新增/修改 Request VO")
@Data
public class ErpAssetTypeSaveReqVO {

    @Schema(description = "类型编号", example = "1")
    private Long id;

    @Schema(description = "父类型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "父类型编号不能为空")
    private Long parentId;

    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "固定资产")
    @NotEmpty(message = "类型名称不能为空")
    private String name;

    @Schema(description = "类型编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "0202")
    @NotEmpty(message = "类型编码不能为空")
    private String code;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "企业的固定资产")
    private String remark;

} 