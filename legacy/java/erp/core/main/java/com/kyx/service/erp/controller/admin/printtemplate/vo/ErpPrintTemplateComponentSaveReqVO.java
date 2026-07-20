package com.kyx.service.erp.controller.admin.printtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 打印模版组件保存 Request VO")
@Data
public class ErpPrintTemplateComponentSaveReqVO {

    @Schema(description = "组件编号", example = "1024")
    private Long id;

    @Schema(description = "模版编号", example = "1")
    private Long templateId;

    @Schema(description = "组件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "qrcode")
    @NotBlank(message = "组件类型不能为空")
    private String type;

    @Schema(description = "组件名称", example = "二维码")
    private String name;

    @Schema(description = "X坐标(mm)", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
    @NotNull(message = "X坐标不能为空")
    private BigDecimal x;

    @Schema(description = "Y坐标(mm)", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
    @NotNull(message = "Y坐标不能为空")
    private BigDecimal y;

    @Schema(description = "宽度(mm)", requiredMode = Schema.RequiredMode.REQUIRED, example = "15.00")
    @NotNull(message = "宽度不能为空")
    private BigDecimal width;

    @Schema(description = "高度(mm)", requiredMode = Schema.RequiredMode.REQUIRED, example = "15.00")
    @NotNull(message = "高度不能为空")
    private BigDecimal height;

    @Schema(description = "组件内容/数据源", example = "${assetId}")
    private String content;

    @Schema(description = "样式配置JSON", example = "{\"fontSize\":12}")
    private String styleJson;

    @Schema(description = "层级排序", example = "1")
    private Integer sort;

} 