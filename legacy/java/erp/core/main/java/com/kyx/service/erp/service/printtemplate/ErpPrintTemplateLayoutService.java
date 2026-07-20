package com.kyx.service.erp.service.printtemplate;

import com.kyx.service.erp.controller.admin.printtemplate.vo.ErpPrintTemplateLayoutConfigVO;

/**
 * ERP 打印模版布局配置 Service 接口
 *
 * @author kyx
 */
public interface ErpPrintTemplateLayoutService {

    /**
     * 获得模版布局配置
     *
     * @param templateId 模版编号
     * @return 布局配置
     */
    ErpPrintTemplateLayoutConfigVO getTemplateLayoutConfig(Long templateId);

    /**
     * 保存模版布局配置
     *
     * @param templateId 模版编号
     * @param layoutConfig 布局配置
     */
    void saveTemplateLayoutConfig(Long templateId, ErpPrintTemplateLayoutConfigVO layoutConfig);

    /**
     * 生成模版预览HTML
     *
     * @param templateId 模版编号
     * @param sampleData 示例数据
     * @return 预览HTML
     */
    String generateTemplatePreviewHtml(Long templateId, Object sampleData);

    /**
     * 生成模版打印HTML
     *
     * @param templateId 模版编号
     * @param printData 打印数据
     * @return 打印HTML
     */
    String generateTemplatePrintHtml(Long templateId, Object printData);

} 