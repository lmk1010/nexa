package com.kyx.service.erp.service.printtemplate;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyx.foundation.common.util.object.BeanUtils;
import com.kyx.service.erp.controller.admin.printtemplate.vo.ErpPrintTemplateLayoutConfigVO;
import com.kyx.service.erp.dal.dataobject.printtemplate.ErpPrintTemplateDO;
import com.kyx.service.erp.dal.dataobject.printtemplate.ErpPrintTemplateComponentDO;
import com.kyx.service.erp.dal.mysql.printtemplate.ErpPrintTemplateMapper;
import com.kyx.service.erp.dal.mysql.printtemplate.ErpPrintTemplateComponentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ERP 打印模版布局配置 Service 实现类
 *
 * @author kyx
 */
@Service
@Slf4j
public class ErpPrintTemplateLayoutServiceImpl implements ErpPrintTemplateLayoutService {

    @Resource
    private ErpPrintTemplateMapper printTemplateMapper;

    @Resource
    private ErpPrintTemplateComponentMapper printTemplateComponentMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ErpPrintTemplateLayoutConfigVO getTemplateLayoutConfig(Long templateId) {
        // 获取模版信息
        ErpPrintTemplateDO template = printTemplateMapper.selectById(templateId);
        if (template == null) {
            return null;
        }

        // 获取组件信息
        List<ErpPrintTemplateComponentDO> componentDOs = printTemplateComponentMapper.selectListByTemplateId(templateId);

        // 构建布局配置
        ErpPrintTemplateLayoutConfigVO layoutConfig = new ErpPrintTemplateLayoutConfigVO();

        // 模版信息
        ErpPrintTemplateLayoutConfigVO.TemplateInfo templateInfo = new ErpPrintTemplateLayoutConfigVO.TemplateInfo();
        templateInfo.setName(template.getName());
        templateInfo.setType(template.getType());
        templateInfo.setWidth(template.getWidth());
        templateInfo.setHeight(template.getHeight());
        templateInfo.setDescription(template.getDescription());
        layoutConfig.setTemplateInfo(templateInfo);

        // 组件信息
        List<ErpPrintTemplateLayoutConfigVO.ComponentInfo> components = componentDOs.stream()
                .map(this::convertToComponentInfo)
                .collect(Collectors.toList());
        layoutConfig.setComponents(components);

        // 布局配置（从模版的configJson中解析）
        ErpPrintTemplateLayoutConfigVO.LayoutConfig layoutConfigInfo = parseLayoutConfig(template.getConfigJson());
        layoutConfig.setLayoutConfig(layoutConfigInfo);

        return layoutConfig;
    }

    @Override
    public void saveTemplateLayoutConfig(Long templateId, ErpPrintTemplateLayoutConfigVO layoutConfig) {
        // 更新模版基本信息
        if (layoutConfig.getTemplateInfo() != null) {
            ErpPrintTemplateDO templateUpdate = new ErpPrintTemplateDO();
            templateUpdate.setId(templateId);
            templateUpdate.setName(layoutConfig.getTemplateInfo().getName());
            templateUpdate.setType(layoutConfig.getTemplateInfo().getType());
            templateUpdate.setWidth(layoutConfig.getTemplateInfo().getWidth());
            templateUpdate.setHeight(layoutConfig.getTemplateInfo().getHeight());
            templateUpdate.setDescription(layoutConfig.getTemplateInfo().getDescription());
            
            // 保存布局配置到configJson
            if (layoutConfig.getLayoutConfig() != null) {
                try {
                    String configJson = objectMapper.writeValueAsString(layoutConfig.getLayoutConfig());
                    templateUpdate.setConfigJson(configJson);
                } catch (JsonProcessingException e) {
                    log.error("保存布局配置失败", e);
                }
            }
            
            printTemplateMapper.updateById(templateUpdate);
        }

        // 更新组件信息
        if (CollUtil.isNotEmpty(layoutConfig.getComponents())) {
            // 先删除现有组件
            printTemplateComponentMapper.deleteByTemplateId(templateId);
            
            // 添加新组件
            for (int i = 0; i < layoutConfig.getComponents().size(); i++) {
                ErpPrintTemplateLayoutConfigVO.ComponentInfo componentInfo = layoutConfig.getComponents().get(i);
                ErpPrintTemplateComponentDO componentDO = convertToComponentDO(templateId, componentInfo, i + 1);
                printTemplateComponentMapper.insert(componentDO);
            }
        }
    }

    @Override
    public String generateTemplatePreviewHtml(Long templateId, Object sampleData) {
        ErpPrintTemplateLayoutConfigVO layoutConfig = getTemplateLayoutConfig(templateId);
        if (layoutConfig == null) {
            return "";
        }

        return generateHtml(layoutConfig, sampleData, false);
    }

    @Override
    public String generateTemplatePrintHtml(Long templateId, Object printData) {
        log.info("开始生成打印HTML，模版ID: {}, 数据类型: {}", templateId, printData != null ? printData.getClass().getSimpleName() : "null");
        
        ErpPrintTemplateLayoutConfigVO layoutConfig = getTemplateLayoutConfig(templateId);
        if (layoutConfig == null) {
            log.error("模版配置为空，模版ID: {}", templateId);
            return "";
        }

        log.info("模版配置加载成功，组件数量: {}", layoutConfig.getComponents() != null ? layoutConfig.getComponents().size() : 0);
        return generateHtml(layoutConfig, printData, true);
    }

    /**
     * 转换组件DO为VO
     */
    private ErpPrintTemplateLayoutConfigVO.ComponentInfo convertToComponentInfo(ErpPrintTemplateComponentDO componentDO) {
        ErpPrintTemplateLayoutConfigVO.ComponentInfo componentInfo = new ErpPrintTemplateLayoutConfigVO.ComponentInfo();
        componentInfo.setId(componentDO.getId().toString());
        componentInfo.setType(componentDO.getType());
        componentInfo.setName(componentDO.getName());
        componentInfo.setX(componentDO.getX());
        componentInfo.setY(componentDO.getY());
        componentInfo.setWidth(componentDO.getWidth());
        componentInfo.setHeight(componentDO.getHeight());
        componentInfo.setContent(componentDO.getContent());
        componentInfo.setZIndex(componentDO.getSort());
        componentInfo.setVisible(true);

        // 解析样式JSON
        if (StrUtil.isNotBlank(componentDO.getStyleJson())) {
            try {
                Map<String, Object> style = objectMapper.readValue(componentDO.getStyleJson(), Map.class);
                componentInfo.setStyle(style);
            } catch (JsonProcessingException e) {
                log.error("解析组件样式失败", e);
                componentInfo.setStyle(new HashMap<>());
            }
        } else {
            componentInfo.setStyle(new HashMap<>());
        }

        return componentInfo;
    }

    /**
     * 转换组件VO为DO
     */
    private ErpPrintTemplateComponentDO convertToComponentDO(Long templateId, ErpPrintTemplateLayoutConfigVO.ComponentInfo componentInfo, int sort) {
        ErpPrintTemplateComponentDO componentDO = new ErpPrintTemplateComponentDO();
        componentDO.setTemplateId(templateId);
        componentDO.setType(componentInfo.getType());
        componentDO.setName(componentInfo.getName());
        componentDO.setX(componentInfo.getX());
        componentDO.setY(componentInfo.getY());
        componentDO.setWidth(componentInfo.getWidth());
        componentDO.setHeight(componentInfo.getHeight());
        componentDO.setContent(componentInfo.getContent());
        componentDO.setSort(componentInfo.getZIndex() != null ? componentInfo.getZIndex() : sort);

        // 序列化样式
        if (componentInfo.getStyle() != null && !componentInfo.getStyle().isEmpty()) {
            try {
                String styleJson = objectMapper.writeValueAsString(componentInfo.getStyle());
                componentDO.setStyleJson(styleJson);
            } catch (JsonProcessingException e) {
                log.error("序列化组件样式失败", e);
            }
        }

        return componentDO;
    }

    /**
     * 解析布局配置
     */
    private ErpPrintTemplateLayoutConfigVO.LayoutConfig parseLayoutConfig(String configJson) {
        ErpPrintTemplateLayoutConfigVO.LayoutConfig layoutConfig = new ErpPrintTemplateLayoutConfigVO.LayoutConfig();
        
        if (StrUtil.isNotBlank(configJson)) {
            try {
                layoutConfig = objectMapper.readValue(configJson, ErpPrintTemplateLayoutConfigVO.LayoutConfig.class);
            } catch (JsonProcessingException e) {
                log.error("解析布局配置失败", e);
            }
        }

        // 设置默认值
        if (layoutConfig.getGridSize() == null) layoutConfig.setGridSize(5);
        if (layoutConfig.getShowGrid() == null) layoutConfig.setShowGrid(true);
        if (layoutConfig.getEnableSnap() == null) layoutConfig.setEnableSnap(true);
        if (layoutConfig.getSnapTolerance() == null) layoutConfig.setSnapTolerance(2);
        if (layoutConfig.getBackgroundColor() == null) layoutConfig.setBackgroundColor("#ffffff");

        return layoutConfig;
    }

    /**
     * 生成HTML
     */
    private String generateHtml(ErpPrintTemplateLayoutConfigVO layoutConfig, Object data, boolean isPrint) {
        StringBuilder html = new StringBuilder();
        
        ErpPrintTemplateLayoutConfigVO.TemplateInfo templateInfo = layoutConfig.getTemplateInfo();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>").append(templateInfo.getName()).append("</title>");
        html.append("<style>");
        html.append("body { margin: 0; padding: ").append(isPrint ? "0" : "20px").append("; font-family: Arial, sans-serif; background: #f5f5f5; }");
        html.append(".template-container { ");
        html.append("width: ").append(templateInfo.getWidth()).append("mm; ");
        html.append("height: ").append(templateInfo.getHeight()).append("mm; ");
        html.append("position: relative; ");
        html.append("background: ").append(layoutConfig.getLayoutConfig().getBackgroundColor()).append("; ");
        html.append("border: 2px solid #000; ");
        html.append("box-shadow: 0 4px 8px rgba(0,0,0,0.1); ");
        if (!isPrint) {
            html.append("margin: 20px auto; ");
        } else {
            html.append("margin: 0; ");
        }
        html.append("page-break-inside: avoid; ");
        html.append("}");
        html.append(".component { position: absolute; overflow: hidden; display: flex; align-items: center; justify-content: center; }");
        
        // 改进组件样式
        html.append(".qrcode { ");
        html.append("display: flex; flex-direction: column; align-items: center; justify-content: center; ");
        html.append("background: white; border-radius: 4px; padding: 2px; ");
        html.append("}");
        
        html.append(".barcode { ");
        html.append("display: flex; flex-direction: column; align-items: center; justify-content: center; ");
        html.append("background: white; border-radius: 4px; padding: 2px; ");
        html.append("}");
        
        // 打印样式优化
        if (isPrint) {
            html.append("@media print { ");
            html.append("body { margin: 0; padding: 0; background: white; } ");
            html.append(".template-container { border: 2px solid #000; margin: 0; box-shadow: none; } ");
            html.append("@page { margin: 10mm; size: auto; } ");
            html.append("}");
        }
        
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='template-container'>");
        
        // 渲染组件
        if (layoutConfig.getComponents() != null) {
            for (ErpPrintTemplateLayoutConfigVO.ComponentInfo component : layoutConfig.getComponents()) {
                if (Boolean.TRUE.equals(component.getVisible())) {
                    html.append(renderComponent(component, data));
                }
            }
        }
        
        html.append("</div>");
        
        if (isPrint) {
            html.append("<script>window.onload = () => window.print();</script>");
        }
        
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * 渲染单个组件
     */
    private String renderComponent(ErpPrintTemplateLayoutConfigVO.ComponentInfo component, Object data) {
        StringBuilder html = new StringBuilder();
        
        html.append("<div class='component component-").append(component.getType()).append("' style='");
        html.append("left: ").append(component.getX()).append("mm; ");
        html.append("top: ").append(component.getY()).append("mm; ");
        html.append("width: ").append(component.getWidth()).append("mm; ");
        html.append("height: ").append(component.getHeight()).append("mm; ");
        
        // 应用样式
        Map<String, Object> style = component.getStyle();
        if (style != null) {
            if (style.containsKey("fontSize")) {
                html.append("font-size: ").append(style.get("fontSize")).append("px; ");
            }
            if (style.containsKey("color")) {
                html.append("color: ").append(style.get("color")).append("; ");
            }
            if (style.containsKey("fontWeight")) {
                html.append("font-weight: ").append(style.get("fontWeight")).append("; ");
            }
            if (style.containsKey("textAlign")) {
                html.append("text-align: ").append(style.get("textAlign")).append("; ");
            }
        }
        
        html.append("'>");
        
        // 根据组件类型渲染内容
        switch (component.getType()) {
            case "qrcode":
                String qrContent = processContent(component.getContent(), data);
                html.append("<div class='qrcode' style='display: flex; flex-direction: column; align-items: center; justify-content: center;'>");
                // 生成简单的QR码占位符，实际打印时由前端处理
                int qrSize = component.getWidth().multiply(BigDecimal.valueOf(3.78)).intValue();
                html.append("<div class='qr-placeholder' data-content='").append(qrContent).append("' style='");
                html.append("width: ").append(qrSize).append("px; ");
                html.append("height: ").append(qrSize).append("px; ");
                html.append("border: 2px solid #000; ");
                html.append("display: flex; align-items: center; justify-content: center; ");
                html.append("background: white; font-size: 10px; text-align: center; ");
                html.append("'>");
                html.append("QR<br/>").append(qrContent.length() > 10 ? qrContent.substring(0, 10) + "..." : qrContent);
                html.append("</div>");
                html.append("</div>");
                break;
            case "barcode":
                String barcodeContent = processContent(component.getContent(), data);
                html.append("<div class='barcode' style='display: flex; flex-direction: column; align-items: center; justify-content: center;'>");
                // 生成条码占位符，实际打印时由前端处理
                int barcodeWidth = component.getWidth().multiply(BigDecimal.valueOf(3.78)).intValue();
                int barcodeHeight = component.getHeight().multiply(BigDecimal.valueOf(0.7)).multiply(BigDecimal.valueOf(3.78)).intValue();
                html.append("<div class='barcode-placeholder' data-content='").append(barcodeContent).append("' style='");
                html.append("width: ").append(barcodeWidth).append("px; ");
                html.append("height: ").append(barcodeHeight).append("px; ");
                html.append("border: 1px solid #000; ");
                html.append("background: repeating-linear-gradient(90deg, #000 0px, #000 1px, #fff 1px, #fff 3px); ");
                html.append("display: flex; align-items: flex-end; justify-content: center; ");
                html.append("'>");
                html.append("<span style='background: white; font-size: 8px; padding: 1px 2px;'>").append(barcodeContent).append("</span>");
                html.append("</div>");
                html.append("</div>");
                break;
            case "text":
            case "label":
                String textContent = processContent(component.getContent(), data);
                html.append("<div style='");
                html.append("display: flex; align-items: center; justify-content: ");
                // 根据样式设置对齐方式
                String textAlign = "left";
                if (component.getStyle() != null && component.getStyle().containsKey("textAlign")) {
                    textAlign = component.getStyle().get("textAlign").toString();
                }
                switch (textAlign) {
                    case "center":
                        html.append("center");
                        break;
                    case "right":
                        html.append("flex-end");
                        break;
                    default:
                        html.append("flex-start");
                        break;
                }
                html.append("; height: 100%; width: 100%; ");
                html.append("padding: 2px 4px; box-sizing: border-box; ");
                html.append("font-weight: ").append(component.getStyle() != null && component.getStyle().containsKey("fontWeight") ? component.getStyle().get("fontWeight") : "normal").append("; ");
                html.append("'>");
                html.append("<span style='white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'>");
                html.append(textContent);
                html.append("</span>");
                html.append("</div>");
                break;
            case "image":
                html.append("<div style='display: flex; align-items: center; justify-content: center; height: 100%; border: 1px solid #ddd;'>");
                html.append("<span style='color: #ccc;'>图片</span>");
                html.append("</div>");
                break;
            case "line":
                html.append("<div style='width: 100%; height: 100%; background: #000;'></div>");
                break;
        }
        
        html.append("</div>");
        
        return html.toString();
    }

    /**
     * 处理内容（替换变量）
     */
    private String processContent(String content, Object data) {
        if (StrUtil.isBlank(content) || data == null) {
            return content != null ? content : "";
        }

        String result = content;
        log.debug("开始处理内容: {}, 数据类型: {}", content, data.getClass().getSimpleName());
        
        // 处理Map类型数据
        if (data instanceof Map) {
            Map<?, ?> dataMap = (Map<?, ?>) data;
            log.debug("处理Map数据，键数量: {}", dataMap.size());
            for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                String key = entry.getKey().toString();
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                String placeholder = "${" + key + "}";
                if (result.contains(placeholder)) {
                    log.debug("替换变量: {} -> {}", placeholder, value);
                    result = result.replace(placeholder, value);
                }
            }
        } else {
            // 处理普通对象，使用反射获取属性值
            try {
                Class<?> clazz = data.getClass();
                Field[] fields = clazz.getDeclaredFields();
                log.debug("处理对象数据，字段数量: {}", fields.length);
                
                for (Field field : fields) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Object fieldValue = field.get(data);
                    String value = fieldValue != null ? fieldValue.toString() : "";
                    String placeholder = "${" + fieldName + "}";
                    if (result.contains(placeholder)) {
                        log.debug("替换变量: {} -> {}", placeholder, value);
                        result = result.replace(placeholder, value);
                    }
                }
            } catch (Exception e) {
                log.warn("处理对象属性失败: {}", e.getMessage());
            }
        }
        
        log.debug("内容处理完成: {} -> {}", content, result);
        return result;
    }

    /**
     * URL编码工具方法
     */
    private String urlEncode(String content) {
        try {
            return java.net.URLEncoder.encode(content, "UTF-8");
        } catch (Exception e) {
            return content.replaceAll("[^a-zA-Z0-9]", "");
        }
    }

} 