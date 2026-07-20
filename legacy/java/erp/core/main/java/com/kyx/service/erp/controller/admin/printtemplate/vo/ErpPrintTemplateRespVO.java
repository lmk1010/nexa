package com.kyx.service.erp.controller.admin.printtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Schema(description = "管理后台 - ERP 打印模版 Response VO")
@Data
public class ErpPrintTemplateRespVO {

    @Schema(description = "模版编号", example = "1024")
    private Long id;

    @Schema(description = "模版名称", example = "资产标签-标准版")
    private String name;

    @Schema(description = "模版类型", example = "asset-label")
    private String type;

    @Schema(description = "模版宽度(mm)", example = "60.00")
    private BigDecimal width;

    @Schema(description = "模版高度(mm)", example = "40.00")
    private BigDecimal height;

    @Schema(description = "模版描述", example = "标准资产标签模版")
    private String description;

    @Schema(description = "预览图片URL", example = "https://example.com/preview.png")
    private String previewImage;

    @Schema(description = "模版配置JSON", example = "{\"backgroundColor\":\"#ffffff\"}")
    private String configJson;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "排序字段", example = "1")
    private Integer sort;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "模版组件列表")
    private List<ErpPrintTemplateComponentRespVO> components;

    @Schema(description = "组件数量", example = "6")
    private Integer componentCount;

} 