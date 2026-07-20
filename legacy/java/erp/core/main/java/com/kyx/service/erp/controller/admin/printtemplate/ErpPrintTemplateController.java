package com.kyx.service.erp.controller.admin.printtemplate;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.erp.controller.admin.printtemplate.vo.*;
import com.kyx.service.erp.dal.dataobject.printtemplate.ErpPrintTemplateDO;
import com.kyx.service.erp.service.printtemplate.ErpPrintTemplateService;
import com.kyx.service.erp.service.printtemplate.ErpPrintTemplateLayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 打印模版")
@RestController
@RequestMapping("/erp/print-template")
@Validated
public class ErpPrintTemplateController {

    @Resource
    private ErpPrintTemplateService printTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建打印模版")
    @PreAuthorize("@ss.hasPermission('erp:print-template:create')")
    public CommonResult<Long> createPrintTemplate(@Valid @RequestBody ErpPrintTemplateSaveReqVO createReqVO) {
        return success(printTemplateService.createPrintTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新打印模版")
    @PreAuthorize("@ss.hasPermission('erp:print-template:update')")
    public CommonResult<Boolean> updatePrintTemplate(@Valid @RequestBody ErpPrintTemplateSaveReqVO updateReqVO) {
        printTemplateService.updatePrintTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除打印模版")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:print-template:delete')")
    public CommonResult<Boolean> deletePrintTemplate(@RequestParam("id") Long id) {
        printTemplateService.deletePrintTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得打印模版")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:print-template:query')")
    public CommonResult<ErpPrintTemplateDO> getPrintTemplate(@RequestParam("id") Long id) {
        ErpPrintTemplateDO printTemplate = printTemplateService.getPrintTemplate(id);
        return success(printTemplate);
    }

    @GetMapping("/page")
    @Operation(summary = "获得打印模版分页")
    @PreAuthorize("@ss.hasPermission('erp:print-template:query')")
    public CommonResult<PageResult<ErpPrintTemplateRespVO>> getPrintTemplatePage(@Valid ErpPrintTemplatePageReqVO pageReqVO) {
        PageResult<ErpPrintTemplateRespVO> pageResult = printTemplateService.getPrintTemplateVOPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获得启用的打印模版列表")
    @Parameter(name = "type", description = "模版类型", example = "asset-label")
    @PreAuthorize("@ss.hasPermission('erp:print-template:query')")
    public CommonResult<List<ErpPrintTemplateRespVO>> getEnabledPrintTemplateList(@RequestParam(value = "type", required = false) String type) {
        List<ErpPrintTemplateRespVO> list = printTemplateService.getEnabledPrintTemplateList(type);
        return success(list);
    }

    @PostMapping("/copy")
    @Operation(summary = "复制打印模版")
    @PreAuthorize("@ss.hasPermission('erp:print-template:create')")
    public CommonResult<Long> copyPrintTemplate(@RequestParam("id") Long id, @RequestParam("name") String name) {
        return success(printTemplateService.copyPrintTemplate(id, name));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新打印模版状态")
    @PreAuthorize("@ss.hasPermission('erp:print-template:update')")
    public CommonResult<Boolean> updatePrintTemplateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        printTemplateService.updatePrintTemplateStatus(id, status);
        return success(true);
    }

    @GetMapping("/layout-config")
    @Operation(summary = "获得打印模版布局配置")
    @Parameter(name = "templateId", description = "模版编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:print-template:query')")
    public CommonResult<ErpPrintTemplateLayoutConfigVO> getTemplateLayoutConfig(@RequestParam("templateId") Long templateId) {
        ErpPrintTemplateLayoutConfigVO layoutConfig = printTemplateLayoutService.getTemplateLayoutConfig(templateId);
        return success(layoutConfig);
    }

    @PostMapping("/layout-config")
    @Operation(summary = "保存打印模版布局配置")
    @PreAuthorize("@ss.hasPermission('erp:print-template:update')")
    public CommonResult<Boolean> saveTemplateLayoutConfig(@RequestParam("templateId") Long templateId, 
                                                         @Valid @RequestBody ErpPrintTemplateLayoutConfigVO layoutConfig) {
        printTemplateLayoutService.saveTemplateLayoutConfig(templateId, layoutConfig);
        return success(true);
    }

    @GetMapping("/preview-html")
    @Operation(summary = "生成模版预览HTML")
    @Parameter(name = "templateId", description = "模版编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:print-template:query')")
    public CommonResult<String> generateTemplatePreviewHtml(@RequestParam("templateId") Long templateId,
                                                           @RequestBody(required = false) Object sampleData) {
        String html = printTemplateLayoutService.generateTemplatePreviewHtml(templateId, sampleData);
        return success(html);
    }

    @PostMapping("/print-html")
    @Operation(summary = "生成模版打印HTML")
    @PreAuthorize("@ss.hasPermission('erp:print-template:query')")
    public CommonResult<String> generateTemplatePrintHtml(@RequestParam("templateId") Long templateId,
                                                         @RequestBody Object printData) {
        String html = printTemplateLayoutService.generateTemplatePrintHtml(templateId, printData);
        return success(html);
    }

    @Resource
    private ErpPrintTemplateLayoutService printTemplateLayoutService;

} 