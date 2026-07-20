package com.kyx.service.erp.controller.admin.asset.vo.category;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 资产分类 Response VO")
@Data
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class ErpAssetCategoryRespVO {

    @Schema(description = "分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("分类编号")
    private Long id;

    @Schema(description = "父分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("父分类编号")
    private Long parentId;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公设备")
    @ExcelProperty("分类名称")
    private String name;

    @Schema(description = "分类编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "OFFICE")
    @ExcelProperty("分类编码")
    private String code;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "办公相关设备")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 