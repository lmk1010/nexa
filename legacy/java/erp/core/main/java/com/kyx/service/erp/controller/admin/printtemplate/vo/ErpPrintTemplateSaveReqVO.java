package com.kyx.service.erp.controller.admin.printtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 打印模版保存 Request VO")
@Data
public class ErpPrintTemplateSaveReqVO {

    @Schema(description = "模版编号", example = "1024")
    private Long id;

    @Schema(description = "模版名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "资产标签-标准版")
    @NotBlank(message = "模版名称不能为空")
    private String name;

    @Schema(description = "模版类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "asset-label")
    @NotBlank(message = "模版类型不能为空")
    private String type;

    @Schema(description = "模版宽度(mm)", requiredMode = Schema.RequiredMode.REQUIRED, example = "60.00")
    @NotNull(message = "模版宽度不能为空")
    private BigDecimal width;

    @Schema(description = "模版高度(mm)", requiredMode = Schema.RequiredMode.REQUIRED, example = "40.00")
    @NotNull(message = "模版高度不能为空")
    private BigDecimal height;

    @Schema(description = "模版描述", example = "标准资产标签模版")
    private String description;

    @Schema(description = "预览图片URL", example = "https://example.com/preview.png")
    private String previewImage;

    @Schema(description = "模版配置JSON", example = "{\"backgroundColor\":\"#ffffff\"}")
    private String configJson;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "排序字段", example = "1")
    private Integer sort;

    @Schema(description = "模版组件列表")
    private List<ErpPrintTemplateComponentSaveReqVO> components;

} 