package com.kyx.service.hr.controller.admin.exam;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.http.HttpUtils;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.hr.controller.admin.exam.vo.ExamResultRespVO;
import com.kyx.service.hr.dal.dataobject.exam.ExamDO;
import com.kyx.service.hr.dal.mysql.exam.ExamMapper;
import com.kyx.service.hr.service.exam.ExamResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * HR 考试结果 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - HR 考试结果")
@RestController
@RequestMapping("/hr/exam-result")
@Validated
public class ExamResultController {

    @Resource
    private ExamResultService resultService;
    @Resource
    private ExamMapper examMapper;

    @GetMapping("/list")
    @Operation(summary = "获得考试结果列表")
    @Parameter(name = "examId", description = "考试ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:exam:result:query')")
    public CommonResult<List<ExamResultRespVO>> getResultList(
            @RequestParam("examId") Long examId,
            @RequestParam(value = "publishId", required = false) Long publishId) {
        return success(resultService.getResultList(examId, publishId));
    }

    @PostMapping("/retry-ai-grade")
    @PreAuthorize("@ss.hasPermission('hr:exam:result:query')")
    public CommonResult<Boolean> retryAiGrade(@RequestParam("attemptId") Long attemptId) {
        resultService.retryAiGrade(attemptId);
        return success(true);
    }

    @PostMapping("/retry-ai-grade-batch")
    @PreAuthorize("@ss.hasPermission('hr:exam:result:query')")
    public CommonResult<Boolean> retryAiGradeBatch(@RequestParam("attemptIds") List<Long> attemptIds) {
        resultService.retryAiGradeBatch(attemptIds);
        return success(true);
    }

    @PostMapping("/pause-ai-grade")
    @PreAuthorize("@ss.hasPermission('hr:exam:result:query')")
    public CommonResult<Boolean> pauseAiGrade(@RequestParam("attemptId") Long attemptId) {
        resultService.pauseAiGrade(attemptId);
        return success(true);
    }

    @PostMapping("/pause-ai-grade-batch")
    @PreAuthorize("@ss.hasPermission('hr:exam:result:query')")
    public CommonResult<Boolean> pauseAiGradeBatch(@RequestParam("attemptIds") List<Long> attemptIds) {
        resultService.pauseAiGradeBatch(attemptIds);
        return success(true);
    }

    @PutMapping("/manual-score")
    @PreAuthorize("@ss.hasPermission('hr:exam:result:query')")
    public CommonResult<Boolean> updateManualScore(@RequestParam("answerId") Long answerId,
                                                   @RequestParam("score") Integer score) {
        resultService.updateManualScore(answerId, score);
        return success(true);
    }

    @GetMapping("/export")
    @Operation(summary = "导出考试结果")
    @Parameter(name = "examId", description = "考试ID", required = true)
    @Parameter(name = "publishId", description = "发布批次ID")
    @PreAuthorize("@ss.hasPermission('hr:exam:result:export')")
    public void exportResult(@RequestParam("examId") Long examId,
                             @RequestParam(value = "publishId", required = false) Long publishId,
                             HttpServletResponse response) throws IOException {
        List<ExamResultRespVO> list = resultService.getResultList(examId, publishId);
        String fileName = publishId != null ? "考试结果-批次" + publishId + ".xls" : "考试结果.xls";
        ExcelUtils.write(response, fileName, "数据", ExamResultRespVO.class, list);
    }

    @GetMapping("/export-answer-package")
    @Operation(summary = "按考生导出考试答题记录包")
    @Parameter(name = "examId", description = "考试ID", required = true)
    @Parameter(name = "publishId", description = "发布批次ID")
    @PreAuthorize("@ss.hasPermission('hr:exam:result:export')")
    public void exportAnswerPackage(@RequestParam("examId") Long examId,
                                    @RequestParam(value = "publishId", required = false) Long publishId,
                                    HttpServletResponse response) throws IOException {
        List<ExamResultRespVO> list = resultService.getResultList(examId, publishId);
        ExamDO exam = examId == null ? null : examMapper.selectById(examId);
        String examName = exam != null && exam.getName() != null && !exam.getName().trim().isEmpty()
                ? exam.getName().trim() : "考试";
        String fileName = publishId != null
                ? sanitizeFileName(examName + " 答题记录包_批次" + publishId) + ".zip"
                : sanitizeFileName(examName + " 答题记录包") + ".zip";

        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(fileName));
        response.setContentType("application/zip;charset=UTF-8");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
            Map<String, Integer> usedEntryNameMap = new HashMap<>();
            if (list == null || list.isEmpty()) {
                writeExamReportZipEntry(zipOutputStream, "暂无考试答题记录.xlsx", null, examName, publishId);
            } else {
                for (ExamResultRespVO result : list) {
                    String entryName = buildExamEntryName(result, examName, publishId, usedEntryNameMap);
                    writeExamReportZipEntry(zipOutputStream, entryName, result, examName, publishId);
                }
            }
            zipOutputStream.finish();
        }
    }

    private void writeExamReportZipEntry(ZipOutputStream zipOutputStream, String entryName,
                                         ExamResultRespVO result, String examName, Long publishId)
            throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        try (Workbook workbook = createExamAnswerWorkbook(result, examName, publishId)) {
            workbook.write(zipOutputStream);
        }
        zipOutputStream.closeEntry();
    }

    private Workbook createExamAnswerWorkbook(ExamResultRespVO result, String examName, Long publishId) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("答题记录");
        sheet.setColumnWidth(0, 8 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 46 * 256);
        sheet.setColumnWidth(3, 40 * 256);
        sheet.setColumnWidth(4, 40 * 256);
        sheet.setColumnWidth(5, 10 * 256);
        sheet.setColumnWidth(6, 10 * 256);
        sheet.setColumnWidth(7, 38 * 256);

        ExamReportStyles styles = buildExamReportStyles(workbook);
        String userName = resolveUserName(result);
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28f);
        setCell(titleRow, 0, userName + " " + examName + " 答题记录", styles.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        Row summaryRow = sheet.createRow(1);
        summaryRow.setHeightInPoints(24f);
        setCell(summaryRow, 0, buildExamSummary(result, publishId), styles.summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(22f);
        String[] headers = {"题号", "题型", "题目", "作答", "标准答案", "满分", "得分", "评语/错误"};
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], styles.headerStyle);
        }

        List<ExamResultRespVO.AnswerRespVO> answers = result == null || result.getAnswers() == null
                ? Collections.emptyList() : result.getAnswers();
        int rowIndex = 4;
        if (answers.isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(22f);
            setCell(row, 0, "暂无答题明细", styles.bodyStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 7));
        } else {
            for (ExamResultRespVO.AnswerRespVO answer : answers) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(36f);
                setCell(row, 0, answer.getSortNo(), styles.bodyCenterStyle);
                setCell(row, 1, resolveItemTypeText(answer.getItemType()), styles.bodyCenterStyle);
                setCell(row, 2, text(answer.getTitle()), styles.bodyStyle);
                setCell(row, 3, resolveAnswerContent(answer), styles.bodyStyle);
                setCell(row, 4, resolveJsonAnswerContent(answer.getStandardAnswerJson()), styles.bodyStyle);
                setCell(row, 5, answer.getMaxScore(), styles.bodyCenterStyle);
                setCell(row, 6, resolveAnswerScore(answer), styles.bodyCenterStyle);
                setCell(row, 7, resolveAnswerComment(answer), styles.bodyStyle);
            }
        }
        return workbook;
    }

    private String buildExamSummary(ExamResultRespVO result, Long publishId) {
        if (result == null) {
            return "暂无考试答题记录";
        }
        String submitAt = result.getSubmitAt() == null
                ? "-" : result.getSubmitAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String passText = result.getPassFlag() == null ? "-" : (Boolean.TRUE.equals(result.getPassFlag()) ? "通过" : "未通过");
        String publishText = publishId == null ? "" : "       批次：" + publishId;
        return "考生：" + resolveUserName(result)
                + "       总分：" + text(result.getTotalScore())
                + "       结果：" + passText
                + "       提交时间：" + submitAt
                + publishText;
    }

    private String buildExamEntryName(ExamResultRespVO result, String examName, Long publishId,
                                      Map<String, Integer> usedEntryNameMap) {
        String batchSuffix = publishId == null ? "" : "_批次" + publishId;
        String baseName = sanitizeFileName(resolveUserName(result) + " " + examName + " 答题记录" + batchSuffix);
        return dedupeEntryName(baseName + ".xlsx", usedEntryNameMap);
    }

    private String resolveUserName(ExamResultRespVO result) {
        if (result == null) {
            return "未命名考生";
        }
        String userName = text(result.getUserName()).trim();
        if (!userName.isEmpty()) {
            return userName;
        }
        return result.getUserId() == null ? "未命名考生" : "考生#" + result.getUserId();
    }

    private String resolveAnswerContent(ExamResultRespVO.AnswerRespVO answer) {
        String answerText = text(answer.getAnswerText()).trim();
        if (!answerText.isEmpty()) {
            return answerText;
        }
        String answerJson = text(answer.getAnswerJson()).trim();
        String display = resolveJsonAnswerContent(answerJson);
        return display.isEmpty() ? "-" : display;
    }

    private String resolveJsonAnswerContent(String answerJson) {
        String raw = text(answerJson).trim();
        if (raw.isEmpty()) {
            return "";
        }
        if (!JsonUtils.isJson(raw)) {
            return raw;
        }
        try {
            Object parsed = JsonUtils.parseObject(raw, Object.class);
            List<String> values = new ArrayList<>();
            flattenAnswerValue(parsed, values);
            LinkedHashSet<String> uniqueValues = values.stream()
                    .map(this::text)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            return uniqueValues.isEmpty() ? raw : String.join("、", uniqueValues);
        } catch (Exception ignored) {
            return raw;
        }
    }

    @SuppressWarnings("unchecked")
    private void flattenAnswerValue(Object value, List<String> collector) {
        if (value == null) {
            return;
        }
        if (value instanceof List) {
            for (Object item : (List<Object>) value) {
                flattenAnswerValue(item, collector);
            }
            return;
        }
        if (value instanceof Map) {
            for (Object mapValue : ((Map<Object, Object>) value).values()) {
                flattenAnswerValue(mapValue, collector);
            }
            return;
        }
        collector.add(String.valueOf(value));
    }

    private Integer resolveAnswerScore(ExamResultRespVO.AnswerRespVO answer) {
        return answer.getManualScore() != null ? answer.getManualScore() : answer.getAnswerScore();
    }

    private String resolveAnswerComment(ExamResultRespVO.AnswerRespVO answer) {
        String comment = text(answer.getAiComment()).trim();
        if (!comment.isEmpty()) {
            return comment;
        }
        String error = text(answer.getGradeError()).trim();
        return error.isEmpty() ? "-" : error;
    }

    private String resolveItemTypeText(String itemType) {
        if ("single".equals(itemType)) return "单选";
        if ("multi".equals(itemType)) return "多选";
        if ("judge".equals(itemType)) return "判断";
        if ("blank".equals(itemType) || "fill".equals(itemType)) return "填空";
        if ("short".equals(itemType) || "essay".equals(itemType) || "qa".equals(itemType)) return "简答";
        return text(itemType);
    }

    private String dedupeEntryName(String entryName, Map<String, Integer> usedEntryNameMap) {
        String safeEntryName = sanitizeFileName(entryName);
        if (!safeEntryName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            safeEntryName = safeEntryName + ".xlsx";
        }
        String key = safeEntryName.toLowerCase(Locale.ROOT);
        int count = usedEntryNameMap.getOrDefault(key, 0) + 1;
        usedEntryNameMap.put(key, count);
        if (count == 1) {
            return safeEntryName;
        }
        int suffixIndex = safeEntryName.toLowerCase(Locale.ROOT).lastIndexOf(".xlsx");
        String prefix = suffixIndex > 0 ? safeEntryName.substring(0, suffixIndex) : safeEntryName;
        String suffix = suffixIndex > 0 ? safeEntryName.substring(suffixIndex) : "";
        return prefix + "_" + count + suffix;
    }

    private String sanitizeFileName(String rawName) {
        String value = text(rawName)
                .replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (value.isEmpty()) {
            return "考试答题记录";
        }
        if (value.length() > 120) {
            value = value.substring(0, 120).trim();
        }
        return value;
    }

    private void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : String.valueOf(value));
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private ExamReportStyles buildExamReportStyles(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setFontName("宋体");
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);

        Font bodyFont = workbook.createFont();
        bodyFont.setFontName("宋体");
        bodyFont.setFontHeightInPoints((short) 11);

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(titleStyle);

        CellStyle summaryStyle = workbook.createCellStyle();
        summaryStyle.setFont(bodyFont);
        summaryStyle.setAlignment(HorizontalAlignment.LEFT);
        summaryStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(summaryStyle);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(bodyFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setThinBorder(headerStyle);

        CellStyle bodyStyle = workbook.createCellStyle();
        bodyStyle.setFont(bodyFont);
        bodyStyle.setAlignment(HorizontalAlignment.LEFT);
        bodyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        bodyStyle.setWrapText(true);
        setThinBorder(bodyStyle);

        CellStyle bodyCenterStyle = workbook.createCellStyle();
        bodyCenterStyle.setFont(bodyFont);
        bodyCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        bodyCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        bodyCenterStyle.setWrapText(true);
        setThinBorder(bodyCenterStyle);

        return new ExamReportStyles(titleStyle, summaryStyle, headerStyle, bodyStyle, bodyCenterStyle);
    }

    private void setThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static class ExamReportStyles {
        private final CellStyle titleStyle;
        private final CellStyle summaryStyle;
        private final CellStyle headerStyle;
        private final CellStyle bodyStyle;
        private final CellStyle bodyCenterStyle;

        private ExamReportStyles(CellStyle titleStyle, CellStyle summaryStyle, CellStyle headerStyle,
                                 CellStyle bodyStyle, CellStyle bodyCenterStyle) {
            this.titleStyle = titleStyle;
            this.summaryStyle = summaryStyle;
            this.headerStyle = headerStyle;
            this.bodyStyle = bodyStyle;
            this.bodyCenterStyle = bodyCenterStyle;
        }
    }
}
