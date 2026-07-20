package com.kyx.service.erp.controller.admin.printtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - ERP 打印模版布局配置 VO")
@Data
public class ErpPrintTemplateLayoutConfigVO {

    @Schema(description = "模版基本信息")
    private TemplateInfo templateInfo;

    @Schema(description = "组件列表")
    private List<ComponentInfo> components;

    @Schema(description = "布局配置")
    private LayoutConfig layoutConfig;

    @Data
    public static class TemplateInfo {
        @Schema(description = "模版名称")
        private String name;

        @Schema(description = "模版类型")
        private String type;

        @Schema(description = "模版宽度(mm)")
        private BigDecimal width;

        @Schema(description = "模版高度(mm)")
        private BigDecimal height;

        @Schema(description = "模版描述")
        private String description;
    }

    @Data
    public static class ComponentInfo {
        @Schema(description = "组件ID")
        private String id;

        @Schema(description = "组件类型")
        private String type;

        @Schema(description = "组件名称")
        private String name;

        @Schema(description = "X坐标(mm)")
        private BigDecimal x;

        @Schema(description = "Y坐标(mm)")
        private BigDecimal y;

        @Schema(description = "宽度(mm)")
        private BigDecimal width;

        @Schema(description = "高度(mm)")
        private BigDecimal height;

        @Schema(description = "组件内容")
        private String content;

        @Schema(description = "样式配置")
        private Map<String, Object> style;

        @Schema(description = "层级")
        private Integer zIndex;

        @Schema(description = "是否可见")
        private Boolean visible;
    }

    @Data
    public static class LayoutConfig {
        @Schema(description = "网格大小")
        private Integer gridSize;

        @Schema(description = "是否显示网格")
        private Boolean showGrid;

        @Schema(description = "是否启用对齐")
        private Boolean enableSnap;

        @Schema(description = "对齐容差")
        private Integer snapTolerance;

        @Schema(description = "背景色")
        private String backgroundColor;

        @Schema(description = "边框设置")
        private BorderConfig border;

        @Schema(description = "打印设置")
        private PrintConfig print;
    }

    @Data
    public static class BorderConfig {
        @Schema(description = "是否显示边框")
        private Boolean show;

        @Schema(description = "边框颜色")
        private String color;

        @Schema(description = "边框宽度")
        private Integer width;

        @Schema(description = "边框样式")
        private String style;
    }

    @Data
    public static class PrintConfig {
        @Schema(description = "打印DPI")
        private Integer dpi;

        @Schema(description = "打印质量")
        private String quality;

        @Schema(description = "纸张类型")
        private String paperType;

        @Schema(description = "打印方向")
        private String orientation;

        @Schema(description = "边距设置")
        private Map<String, BigDecimal> margins;
    }

} 