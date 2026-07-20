package com.kyx.service.erp.controller.admin.asset.vo.type;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 资产类型 Response VO")
@Data
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class ErpAssetTypeRespVO {

    @Schema(description = "类型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("类型编号")
    private Long id;

    @Schema(description = "父类型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("父类型编号")
    private Long parentId;

    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "固定资产")
    @ExcelProperty("类型名称")
    private String name;

    @Schema(description = "类型编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "0202")
    @ExcelProperty("类型编码")
    private String code;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "企业的固定资产")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 