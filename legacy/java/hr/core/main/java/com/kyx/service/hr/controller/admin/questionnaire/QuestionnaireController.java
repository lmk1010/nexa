package com.kyx.service.hr.controller.admin.questionnaire;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.pojo.PageResult;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireImportRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnairePageReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireSaveReqVO;
import com.kyx.service.hr.service.questionnaire.QuestionnaireExcelParser;
import com.kyx.service.hr.service.questionnaire.QuestionnaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.kyx.foundation.common.pojo.CommonResult.success;
import static com.kyx.service.hr.enums.ErrorCodeConstants.QUESTIONNAIRE_IMPORT_FAIL;

/**
 * HR 问卷管理 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - HR 问卷管理")
@RestController
@RequestMapping("/hr/questionnaire")
@Validated
public class QuestionnaireController {

    @Resource
    private QuestionnaireService questionnaireService;

    @PostMapping("/create")
    @Operation(summary = "创建问卷")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:create')")
    public CommonResult<Long> createQuestionnaire(@Valid @RequestBody QuestionnaireSaveReqVO createReqVO) {
        return success(questionnaireService.createQuestionnaire(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新问卷")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:update')")
    public CommonResult<Boolean> updateQuestionnaire(@Valid @RequestBody QuestionnaireSaveReqVO updateReqVO) {
        questionnaireService.updateQuestionnaire(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除问卷")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:delete')")
    public CommonResult<Boolean> deleteQuestionnaire(@RequestParam("id") Long id) {
        questionnaireService.deleteQuestionnaire(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得问卷详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:query')")
    public CommonResult<QuestionnaireRespVO> getQuestionnaire(@RequestParam("id") Long id) {
        return success(questionnaireService.getQuestionnaire(id));
    }

    @GetMapping("/accessible-get")
    @Operation(summary = "获得当前用户可访问的问卷详情")
    @Parameter(name = "id", description = "问卷编号", required = true)
    public CommonResult<QuestionnaireRespVO> getAccessibleQuestionnaire(@RequestParam("id") Long id,
                                                                        @RequestParam(value = "assignmentId", required = false) Long assignmentId,
                                                                        @RequestParam(value = "publishId", required = false) Long publishId) {
        return success(questionnaireService.getAccessibleQuestionnaire(id, assignmentId, publishId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得问卷分页")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:query')")
    public CommonResult<PageResult<QuestionnaireRespVO>> getQuestionnairePage(@Valid QuestionnairePageReqVO pageVO) {
        return success(questionnaireService.getQuestionnairePage(pageVO));
    }

    @PostMapping("/import-excel")
    @Operation(summary = "导入问卷模板（Excel）")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:create')")
    public CommonResult<QuestionnaireImportRespVO> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            QuestionnaireImportRespVO resp = QuestionnaireExcelParser.parse(file.getInputStream(), file.getOriginalFilename());
            return success(resp);
        } catch (Exception e) {
            return CommonResult.error(QUESTIONNAIRE_IMPORT_FAIL);
        }
    }

    @GetMapping("/export-template")
    @Operation(summary = "导出问卷模板（Excel）")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:query')")
    public void exportTemplate(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        QuestionnaireRespVO questionnaire = questionnaireService.getQuestionnaire(id);
        if (questionnaire == null) {
            return;
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("模板");
            int rowIndex = 0;
            Row row1 = sheet.createRow(rowIndex++);
            row1.createCell(0).setCellValue(questionnaire.getName());
            Row row2 = sheet.createRow(rowIndex++);
            row2.createCell(0).setCellValue("被考核人：       参与评分人数：人         平均得分：");
            row2.createCell(2).setCellValue("得分");

            List<QuestionnaireRespVO.Item> items = questionnaire.getItems();
            if (items != null) {
                int qNo = 1;
                for (QuestionnaireRespVO.Item item : items) {
                    String title = qNo + ". " + (item.getTitle() == null ? "" : item.getTitle());
                    if (Boolean.TRUE.equals(item.getRequired())) {
                        title += "（必填）";
                    }
                    if (item.getItemType() != null && item.getItemType().startsWith("score") && item.getMaxScore() != null) {
                        title += "（满分" + item.getMaxScore() + "分）";
                    }
                    Row qRow = sheet.createRow(rowIndex++);
                    qRow.createCell(0).setCellValue(title);

                    if ("single".equals(item.getItemType()) || "multi".equals(item.getItemType())) {
                        List<QuestionnaireRespVO.Option> options = item.getOptions();
                        if (options != null) {
                            for (QuestionnaireRespVO.Option opt : options) {
                                String optText = opt.getOptionText() == null ? "" : opt.getOptionText();
                                if (opt.getOptionScore() != null) {
                                    optText += "（" + opt.getOptionScore() + "）";
                                }
                                Row optRow = sheet.createRow(rowIndex++);
                                optRow.createCell(0).setCellValue(optText);
                                optRow.createCell(1).setCellValue(0);
                            }
                        }
                    } else {
                        qRow.createCell(2).setCellValue(0);
                    }
                    qNo++;
                }
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String filename = URLEncoder.encode(questionnaire.getName() + "_模板.xlsx", StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
            workbook.write(response.getOutputStream());
        }
    }

}
