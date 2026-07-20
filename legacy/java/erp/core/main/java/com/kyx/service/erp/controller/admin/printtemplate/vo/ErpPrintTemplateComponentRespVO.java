package com.kyx.service.erp.controller.admin.printtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Schema(description = "管理后台 - ERP 打印模版组件 Response VO")
@Data
public class ErpPrintTemplateComponentRespVO {

    @Schema(description = "组件编号", example = "1024")
    private Long id;

    @Schema(description = "模版编号", example = "1")
    private Long templateId;

    @Schema(description = "组件类型", example = "qrcode")
    private String type;

    @Schema(description = "组件名称", example = "二维码")
    private String name;

    @Schema(description = "X坐标(mm)", example = "10.00")
    private BigDecimal x;

    @Schema(description = "Y坐标(mm)", example = "10.00")
    private BigDecimal y;

    @Schema(description = "宽度(mm)", example = "15.00")
    private BigDecimal width;

    @Schema(description = "高度(mm)", example = "15.00")
    private BigDecimal height;

    @Schema(description = "组件内容/数据源", example = "${assetId}")
    private String content;

    @Schema(description = "样式配置JSON", example = "{\"fontSize\":12}")
    private String styleJson;

    @Schema(description = "层级排序", example = "1")
    private Integer sort;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

} 