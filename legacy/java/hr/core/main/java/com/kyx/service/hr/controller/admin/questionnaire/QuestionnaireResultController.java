package com.kyx.service.hr.controller.admin.questionnaire;

import com.kyx.foundation.common.pojo.CommonResult;
import com.kyx.foundation.common.util.http.HttpUtils;
import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.foundation.excel.core.util.ExcelUtils;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireItemStatRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireResultExportRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireResultRespVO;
import com.kyx.service.hr.dal.dataobject.questionnaire.QuestionnaireOptionDO;
import com.kyx.service.hr.dal.mysql.questionnaire.QuestionnaireOptionMapper;
import com.kyx.service.hr.service.questionnaire.QuestionnaireResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.kyx.foundation.common.pojo.CommonResult.success;

/**
 * HR 问卷结果 Controller
 *
 * @author MK
 */
@Tag(name = "管理后台 - HR 问卷结果")
@RestController
@RequestMapping("/hr/questionnaire-result")
@Validated
public class QuestionnaireResultController {

    @Resource
    private QuestionnaireResultService resultService;
    @Resource
    private QuestionnaireOptionMapper optionMapper;

    @GetMapping("/list")
    @Operation(summary = "获得问卷结果列表")
    @Parameter(name = "publishId", description = "发布ID", required = true)
    @Parameter(name = "batchNo", description = "批次号")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:result:query')")
    public CommonResult<List<QuestionnaireResultRespVO>> getResultList(@RequestParam("publishId") Long publishId,
                                                                        @RequestParam(value = "batchNo", required = false) Integer batchNo) {
        return success(resultService.getResultList(publishId, batchNo));
    }

    @GetMapping("/export")
    @Operation(summary = "导出问卷结果")
    @Parameter(name = "publishId", description = "发布ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:result:export')")
    public void exportResult(@RequestParam("publishId") Long publishId,
                             @RequestParam(value = "batchNo", required = false) Integer batchNo,
                             HttpServletResponse response)
            throws IOException {
        List<QuestionnaireResultExportRespVO> list = resultService.getResultExportList(publishId, batchNo);
        String filename = batchNo == null ? "问卷结果报告_全部批次.xlsx" : "问卷结果报告_第" + batchNo + "期.xlsx";
        Map<Long, List<QuestionnaireOptionDO>> optionMap = buildOptionMap(list);
        try (Workbook workbook = createStyledReportWorkbook(list, batchNo, optionMap)) {
            response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/export-answer-package")
    @Operation(summary = "按人员导出问卷答题记录包")
    @Parameter(name = "publishId", description = "发布ID", required = true)
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:result:export')")
    public void exportAnswerPackage(@RequestParam("publishId") Long publishId,
                                    @RequestParam(value = "batchNo", required = false) Integer batchNo,
                                    HttpServletResponse response)
            throws IOException {
        List<QuestionnaireResultExportRespVO> list = resultService.getResultExportList(publishId, batchNo);
        Map<Long, List<QuestionnaireOptionDO>> optionMap = buildOptionMap(list);
        Map<String, List<QuestionnaireResultExportRespVO>> subjectMap = groupAnswerPackageRows(list);
        String filename = batchNo == null ? "问卷答题记录包_全部批次.zip" : "问卷答题记录包_第" + batchNo + "期.zip";

        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/zip;charset=UTF-8");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
            Map<String, Integer> usedEntryNameMap = new HashMap<>();
            if (subjectMap.isEmpty()) {
                writeAnswerDetailZipEntry(zipOutputStream, "暂无问卷答题记录.xlsx", Collections.emptyList(), batchNo, optionMap);
            } else {
                for (List<QuestionnaireResultExportRespVO> rows : subjectMap.values()) {
                    String entryName = buildAnswerPackageEntryName(rows, usedEntryNameMap);
                    writeAnswerDetailZipEntry(zipOutputStream, entryName, rows, batchNo, optionMap);
                }
            }
            zipOutputStream.finish();
        }
    }

    private Workbook createStyledReportWorkbook(List<QuestionnaireResultExportRespVO> list, Integer batchNo,
                                                Map<Long, List<QuestionnaireOptionDO>> optionMap) {
        Workbook workbook = new XSSFWorkbook();
        List<ReportSheetGroup> groups = buildReportSheetGroups(list);

        Sheet summarySheet = workbook.createSheet("汇总");
        writeReportSummarySheet(workbook, summarySheet, groups, list, batchNo, buildSummaryStyles(workbook));

        ReportStyles reportStyles = buildStyles(workbook);
        if (groups.isEmpty()) {
            Sheet sheet = workbook.createSheet("答题记录");
            applyStyledReportColumnWidths(sheet);
            writeStyledReport(sheet, Collections.emptyList(), batchNo, optionMap, reportStyles);
            return workbook;
        }
        for (ReportSheetGroup group : groups) {
            Sheet sheet = workbook.createSheet(group.getSheetName());
            applyStyledReportColumnWidths(sheet);
            writeStyledReport(sheet, group.getRows(), batchNo, optionMap, reportStyles);
        }
        return workbook;
    }

    private void applyStyledReportColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, (int) (115.125 * 256));
        sheet.setColumnWidth(1, (int) (15.375 * 256));
        sheet.setColumnWidth(2, (int) (18.25 * 256));
    }

    private void writeAnswerDetailZipEntry(ZipOutputStream zipOutputStream, String entryName,
                                           List<QuestionnaireResultExportRespVO> rows, Integer batchNo,
                                           Map<Long, List<QuestionnaireOptionDO>> optionMap)
            throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        try (Workbook workbook = createAnswerDetailWorkbook(rows, batchNo, optionMap)) {
            workbook.write(zipOutputStream);
        }
        zipOutputStream.closeEntry();
    }

    private Workbook createAnswerDetailWorkbook(List<QuestionnaireResultExportRespVO> rows, Integer batchNo,
                                                Map<Long, List<QuestionnaireOptionDO>> optionMap) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("答题记录");
        AnswerDetailStyles styles = buildAnswerDetailStyles(workbook);
        List<QuestionnaireResultExportRespVO> safeRows = rows == null ? Collections.emptyList() : rows.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((QuestionnaireResultExportRespVO r) -> r.getBatchNo() == null ? Integer.MAX_VALUE : r.getBatchNo())
                        .thenComparing(r -> text(r.getEvaluatorName()))
                        .thenComparing(r -> r.getAssignmentId() == null ? Long.MAX_VALUE : r.getAssignmentId())
                        .thenComparing(r -> r.getItemSortNo() == null ? Integer.MAX_VALUE : r.getItemSortNo())
                        .thenComparing(r -> r.getItemId() == null ? Long.MAX_VALUE : r.getItemId()))
                .collect(Collectors.toList());
        boolean peerReport = safeRows.stream().anyMatch(this::isPeerExportRow);
        int columnCount = peerReport ? 12 : 11;
        applyAnswerDetailColumnWidths(sheet, peerReport);

        String subjectName = safeRows.isEmpty() ? "问卷" : resolveAnswerPackageSubjectName(safeRows.get(0));
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28f);
        setCell(titleRow, 0, safeRows.isEmpty() ? "问卷答题记录" : buildReportTitle(safeRows, subjectName), styles.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));

        Row summaryRow = sheet.createRow(1);
        summaryRow.setHeightInPoints(24f);
        setCell(summaryRow, 0, buildAnswerDetailSummary(safeRows, subjectName, peerReport), styles.summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnCount - 1));

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(22f);
        String[] headers = buildAnswerDetailHeaders(peerReport);
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], styles.headerStyle);
        }

        int rowIndex = 4;
        if (safeRows.isEmpty()) {
            Row emptyRow = sheet.createRow(rowIndex);
            emptyRow.setHeightInPoints(24f);
            setCell(emptyRow, 0, "暂无可导出的问卷数据", styles.bodyStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columnCount - 1));
        } else {
            int index = 1;
            for (QuestionnaireResultExportRespVO rowData : safeRows) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(36f);
                writeAnswerDetailRow(row, rowData, index++, peerReport, optionMap, styles);
            }
            sheet.setAutoFilter(new CellRangeAddress(3, rowIndex - 1, 0, columnCount - 1));
            sheet.createFreezePane(0, 4);
        }
        return workbook;
    }

    private void applyAnswerDetailColumnWidths(Sheet sheet, boolean peerReport) {
        int[] widths = peerReport
                ? new int[]{8, 14, 16, 16, 12, 20, 8, 12, 46, 34, 10, 12}
                : new int[]{8, 14, 16, 12, 20, 8, 12, 46, 34, 10, 12};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    private String[] buildAnswerDetailHeaders(boolean peerReport) {
        if (peerReport) {
            return new String[]{"序号", "批次", "被评价人", "评价人", "状态", "提交时间",
                    "题号", "题型", "题目", "作答", "得分", "答卷总分"};
        }
        return new String[]{"序号", "批次", "填写人", "状态", "提交时间",
                "题号", "题型", "题目", "作答", "得分", "答卷总分"};
    }

    private void writeAnswerDetailRow(Row row, QuestionnaireResultExportRespVO rowData, int index, boolean peerReport,
                                      Map<Long, List<QuestionnaireOptionDO>> optionMap, AnswerDetailStyles styles) {
        int col = 0;
        setCell(row, col++, index, styles.centerStyle);
        setCell(row, col++, resolveRowBatchDisplay(rowData), styles.centerStyle);
        setCell(row, col++, peerReport ? resolveAnswerPackageSubjectName(rowData) : resolveEvaluatorLabel(rowData), styles.bodyStyle);
        if (peerReport) {
            setCell(row, col++, resolveEvaluatorLabel(rowData), styles.bodyStyle);
        }
        setCell(row, col++, text(rowData.getStatusText()).trim(), styles.centerStyle);
        setCell(row, col++, formatSubmitTime(rowData), styles.centerStyle);
        setCell(row, col++, rowData.getItemSortNo(), styles.centerStyle);
        setCell(row, col++, text(rowData.getItemTypeText()).trim(), styles.centerStyle);
        setCell(row, col++, text(rowData.getItemTitle()).trim(), styles.bodyStyle);
        setCell(row, col++, resolveAnswerDisplay(rowData, optionMap == null ? null : optionMap.get(rowData.getItemId())), styles.bodyStyle);
        setCell(row, col++, rowData.getItemScore(), styles.centerStyle);
        setCell(row, col, rowData.getTotalScore(), styles.centerStyle);
    }

    private String buildAnswerDetailSummary(List<QuestionnaireResultExportRespVO> rows, String subjectName, boolean peerReport) {
        if (rows == null || rows.isEmpty()) {
            return "暂无问卷答题记录";
        }
        Set<Long> assignmentIds = rows.stream()
                .map(QuestionnaireResultExportRespVO::getAssignmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> submittedAssignmentIds = rows.stream()
                .filter(r -> "已提交".equals(r.getStatusText()))
                .map(QuestionnaireResultExportRespVO::getAssignmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Integer> assignmentTotalMap = rows.stream()
                .filter(r -> r.getAssignmentId() != null)
                .collect(Collectors.toMap(QuestionnaireResultExportRespVO::getAssignmentId,
                        r -> r.getTotalScore() == null ? 0 : r.getTotalScore(), (l, r) -> l));
        double avgScore = submittedAssignmentIds.isEmpty() ? 0.0 : submittedAssignmentIds.stream()
                .map(id -> assignmentTotalMap.getOrDefault(id, 0))
                .mapToInt(Integer::intValue)
                .average().orElse(0.0);
        String countText = peerReport
                ? "       已评/应评：" + submittedAssignmentIds.size() + "/" + assignmentIds.size() + "人"
                : "       已填/应填：" + submittedAssignmentIds.size() + "/" + assignmentIds.size() + "次";
        return (peerReport ? "被评价人：" : "填写人：") + subjectName
                + countText
                + "       平均得分：" + formatAvg(avgScore)
                + buildBatchDisplayText(rows);
    }

    private String formatSubmitTime(QuestionnaireResultExportRespVO rowData) {
        if (rowData == null || rowData.getSubmitTime() == null) {
            return "-";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(rowData.getSubmitTime());
    }

    private String resolveAnswerDisplay(QuestionnaireResultExportRespVO row, List<QuestionnaireOptionDO> options) {
        if (row == null) {
            return "-";
        }
        if (!"已提交".equals(row.getStatusText())) {
            return "未填写";
        }
        String content = text(row.getAnswerContent()).trim();
        Map<String, String> optionAliasMap = buildOptionAliasMap(options);
        String itemTypeText = text(row.getItemTypeText()).trim();
        if ("多选".equals(itemTypeText)) {
            String mapped = mapAnswerValues(splitMultiAnswers(content), optionAliasMap);
            if (!mapped.isEmpty()) {
                return mapped;
            }
        } else if ("单选".equals(itemTypeText)) {
            String mapped = mapAnswerValues(splitSingleAnswers(content), optionAliasMap);
            if (!mapped.isEmpty()) {
                return mapped;
            }
        }
        if (!content.isEmpty()) {
            return content;
        }
        return row.getItemScore() == null ? "-" : String.valueOf(row.getItemScore());
    }

    private String mapAnswerValues(List<String> values, Map<String, String> optionAliasMap) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String answer = resolveAnswerKey(value, optionAliasMap);
            if (!answer.isEmpty()) {
                result.add(answer);
            }
        }
        return String.join("、", result);
    }

    private List<ReportSheetGroup> buildReportSheetGroups(List<QuestionnaireResultExportRespVO> list) {
        Map<String, List<QuestionnaireResultExportRespVO>> groupedRows = groupStyledReportRows(list);
        List<ReportSheetGroup> groups = new ArrayList<>();
        Map<String, Integer> usedSheetNameMap = new HashMap<>();
        int index = 1;
        for (List<QuestionnaireResultExportRespVO> rows : groupedRows.values()) {
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            String subjectName = resolveAnswerPackageSubjectName(rows.get(0));
            String sheetName = dedupeSheetName(String.format(Locale.CHINA, "%02d-%s", index, subjectName),
                    usedSheetNameMap);
            groups.add(new ReportSheetGroup(subjectName, sheetName, rows));
            index++;
        }
        return groups;
    }

    private Map<String, List<QuestionnaireResultExportRespVO>> groupStyledReportRows(List<QuestionnaireResultExportRespVO> list) {
        if (list == null || list.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(this::resolveAnswerPackageSubjectName)
                        .thenComparing(r -> r.getBatchNo() == null ? Integer.MAX_VALUE : r.getBatchNo())
                        .thenComparing(r -> r.getItemSortNo() == null ? Integer.MAX_VALUE : r.getItemSortNo())
                        .thenComparing(r -> text(r.getEvaluatorName()))
                        .thenComparing(r -> r.getAssignmentId() == null ? Long.MAX_VALUE : r.getAssignmentId()))
                .collect(Collectors.groupingBy(this::resolveStyledReportSubjectKey,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private String resolveStyledReportSubjectKey(QuestionnaireResultExportRespVO row) {
        boolean peerReport = isPeerExportRow(row);
        Long personId = peerReport ? row.getTargetId() : row.getEvaluatorId();
        String id = personId == null ? "" : String.valueOf(personId);
        String name = resolveAnswerPackageSubjectName(row);
        if (id.isEmpty() && ("未命名人员".equals(name) || name.isEmpty()) && row != null && row.getAssignmentId() != null) {
            id = "assignment_" + row.getAssignmentId();
        }
        return (peerReport ? "peer_" : "normal_") + id + "_" + name;
    }

    private void writeReportSummarySheet(Workbook workbook, Sheet sheet, List<ReportSheetGroup> groups,
                                         List<QuestionnaireResultExportRespVO> allRows, Integer batchNo,
                                         SummaryStyles styles) {
        applyReportSummaryColumnWidths(sheet);
        int columnCount = 9;
        String questionnaireName = resolveQuestionnaireName(allRows);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28f);
        setCell(titleRow, 0, questionnaireName + " 导出汇总", styles.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnCount - 1));

        Row summaryRow = sheet.createRow(1);
        summaryRow.setHeightInPoints(24f);
        setCell(summaryRow, 0, buildReportSummaryText(groups, allRows, batchNo), styles.summaryStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnCount - 1));

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(22f);
        String[] headers = {"序号", "类型", "人员", "批次", "应提交", "已提交", "未提交", "平均得分", "详情Sheet"};
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], styles.headerStyle);
        }

        int rowIndex = 4;
        if (groups == null || groups.isEmpty()) {
            Row emptyRow = sheet.createRow(rowIndex);
            emptyRow.setHeightInPoints(24f);
            setCell(emptyRow, 0, "暂无可导出的问卷数据", styles.bodyStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columnCount - 1));
            return;
        }

        int index = 1;
        for (ReportSheetGroup group : groups) {
            QuestionnaireResultExportRespVO firstRow = group.getRows().get(0);
            ReportSubjectStats stats = buildReportSubjectStats(group.getRows());
            Row row = sheet.createRow(rowIndex++);
            row.setHeightInPoints(22f);
            setCell(row, 0, index++, styles.centerStyle);
            setCell(row, 1, isPeerExportRow(firstRow) ? "被评价人" : "填写人", styles.centerStyle);
            setCell(row, 2, group.getSubjectName(), styles.bodyStyle);
            setCell(row, 3, resolveBatchDisplay(group.getRows()), styles.centerStyle);
            setCell(row, 4, stats.getExpectedCount(), styles.centerStyle);
            setCell(row, 5, stats.getSubmittedCount(), styles.centerStyle);
            setCell(row, 6, stats.getPendingCount(), styles.centerStyle);
            setCell(row, 7, formatAvg(stats.getAverageScore()), styles.centerStyle);
            Cell linkCell = row.createCell(8);
            linkCell.setCellValue(group.getSheetName());
            linkCell.setCellStyle(styles.linkStyle);
            Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.DOCUMENT);
            link.setAddress("'" + group.getSheetName().replace("'", "''") + "'!A1");
            linkCell.setHyperlink(link);
        }
        sheet.setAutoFilter(new CellRangeAddress(3, rowIndex - 1, 0, columnCount - 1));
        sheet.createFreezePane(0, 4);
    }

    private void applyReportSummaryColumnWidths(Sheet sheet) {
        int[] widths = {8, 12, 20, 14, 10, 10, 10, 12, 26};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    private String buildReportSummaryText(List<ReportSheetGroup> groups, List<QuestionnaireResultExportRespVO> allRows,
                                          Integer batchNo) {
        int personCount = groups == null ? 0 : groups.size();
        ReportSubjectStats totalStats = buildReportSubjectStats(allRows == null ? Collections.emptyList() : allRows);
        String batchText;
        if (batchNo != null) {
            batchText = batchNo <= 0 ? "未分期" : "第" + batchNo + "期";
        } else {
            batchText = resolveBatchDisplay(allRows);
            if (batchText.isEmpty()) {
                batchText = "全部批次";
            }
        }
        return "批次：" + batchText
                + "       人员数：" + personCount
                + "       已提交/应提交：" + totalStats.getSubmittedCount() + "/" + totalStats.getExpectedCount()
                + "       平均得分：" + formatAvg(totalStats.getAverageScore());
    }

    private ReportSubjectStats buildReportSubjectStats(List<QuestionnaireResultExportRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ReportSubjectStats(0, 0, 0, 0.0);
        }
        Set<Long> assignmentIds = rows.stream()
                .map(QuestionnaireResultExportRespVO::getAssignmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> submittedAssignmentIds = rows.stream()
                .filter(r -> "已提交".equals(r.getStatusText()))
                .map(QuestionnaireResultExportRespVO::getAssignmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Integer> assignmentTotalMap = rows.stream()
                .filter(r -> r.getAssignmentId() != null)
                .collect(Collectors.toMap(QuestionnaireResultExportRespVO::getAssignmentId,
                        r -> r.getTotalScore() == null ? 0 : r.getTotalScore(), (l, r) -> l));
        double avgScore = submittedAssignmentIds.isEmpty() ? 0.0 : submittedAssignmentIds.stream()
                .map(id -> assignmentTotalMap.getOrDefault(id, 0))
                .mapToInt(Integer::intValue)
                .average().orElse(0.0);
        int expectedCount = assignmentIds.size();
        int submittedCount = submittedAssignmentIds.size();
        return new ReportSubjectStats(expectedCount, submittedCount,
                Math.max(expectedCount - submittedCount, 0), avgScore);
    }

    private String dedupeSheetName(String rawName, Map<String, Integer> usedSheetNameMap) {
        String baseName = sanitizeSheetName(rawName);
        String candidate = baseName;
        int count = 1;
        while (usedSheetNameMap.containsKey(candidate.toLowerCase(Locale.ROOT))) {
            count++;
            String suffix = "_" + count;
            int maxBaseLength = Math.max(1, 31 - suffix.length());
            candidate = truncateSheetName(baseName, maxBaseLength) + suffix;
        }
        usedSheetNameMap.put(candidate.toLowerCase(Locale.ROOT), count);
        return candidate;
    }

    private String sanitizeSheetName(String rawName) {
        String value = text(rawName)
                .replaceAll("[\\\\/:*?\\[\\]\\r\\n\\t]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        while (value.startsWith("'")) {
            value = value.substring(1).trim();
        }
        while (value.endsWith("'")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.isEmpty()) {
            value = "答题记录";
        }
        return truncateSheetName(value, 31);
    }

    private String truncateSheetName(String value, int maxLength) {
        String safeValue = text(value).trim();
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        return safeValue.substring(0, maxLength).trim();
    }

    private Map<String, List<QuestionnaireResultExportRespVO>> groupAnswerPackageRows(List<QuestionnaireResultExportRespVO> list) {
        if (list == null || list.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(this::resolveAnswerPackageSubjectName)
                        .thenComparing(r -> r.getBatchNo() == null ? Integer.MAX_VALUE : r.getBatchNo())
                        .thenComparing(r -> r.getItemSortNo() == null ? Integer.MAX_VALUE : r.getItemSortNo())
                        .thenComparing(r -> text(r.getEvaluatorName()))
                        .thenComparing(r -> r.getAssignmentId() == null ? Long.MAX_VALUE : r.getAssignmentId()))
                .collect(Collectors.groupingBy(this::resolveAnswerPackageSubjectKey,
                        LinkedHashMap::new, Collectors.toList()));
    }

    private String resolveAnswerPackageSubjectKey(QuestionnaireResultExportRespVO row) {
        boolean peerReport = isPeerExportRow(row);
        String id = peerReport ? text(row.getTargetId()) : text(row.getAssignmentId());
        String name = resolveAnswerPackageSubjectName(row);
        return (peerReport ? "peer_" : "normal_") + id + "_" + name;
    }

    private String resolveAnswerPackageSubjectName(QuestionnaireResultExportRespVO row) {
        if (row == null) {
            return "未命名人员";
        }
        String name = isPeerExportRow(row) ? text(row.getTargetName()).trim() : text(row.getEvaluatorName()).trim();
        if (!name.isEmpty()) {
            return name;
        }
        Long id = isPeerExportRow(row) ? row.getTargetId() : row.getEvaluatorId();
        if (id != null) {
            return (isPeerExportRow(row) ? "被评价人#" : "填写人#") + id;
        }
        return "未命名人员";
    }

    private String buildAnswerPackageEntryName(List<QuestionnaireResultExportRespVO> rows, Map<String, Integer> usedEntryNameMap) {
        QuestionnaireResultExportRespVO firstRow = rows == null || rows.isEmpty() ? null : rows.get(0);
        String subjectName = resolveAnswerPackageSubjectName(firstRow);
        String questionnaireName = resolveQuestionnaireName(rows);
        String baseName = sanitizeFileName(subjectName + " " + questionnaireName + " 答题记录" + resolveBatchSuffix(rows));
        return dedupeEntryName(baseName + ".xlsx", usedEntryNameMap);
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
            return "问卷答题记录";
        }
        if (value.length() > 120) {
            value = value.substring(0, 120).trim();
        }
        return value;
    }

    private void writeStyledReport(Sheet sheet, List<QuestionnaireResultExportRespVO> list, Integer batchNo,
                                   Map<Long, List<QuestionnaireOptionDO>> optionMap, ReportStyles styles) {
        int rowIndex = 0;
        if (list == null || list.isEmpty()) {
            rowIndex = appendMergedRow(sheet, rowIndex, "暂无可导出的问卷数据", styles.subTitleStyle, 24f);
            return;
        }
        Map<Integer, List<QuestionnaireResultExportRespVO>> batchMap = list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(r -> r.getBatchNo() == null ? 0 : r.getBatchNo()));
        List<Integer> batchNos = new ArrayList<>(batchMap.keySet());
        batchNos.sort(Integer::compareTo);
        boolean showBatchTitle = batchNo == null && batchNos.size() > 1;

        for (int i = 0; i < batchNos.size(); i++) {
            Integer currentBatchNo = batchNos.get(i);
            List<QuestionnaireResultExportRespVO> batchRows = batchMap.getOrDefault(currentBatchNo, new ArrayList<>());
            if (showBatchTitle) {
                String title = currentBatchNo <= 0 ? "未分期问卷结果" : "第" + currentBatchNo + "期问卷结果";
                rowIndex = appendMergedRow(sheet, rowIndex, title, styles.batchTitleStyle, 24f);
            }
            boolean peerReport = batchRows.stream().anyMatch(this::isPeerExportRow);
            Map<String, List<QuestionnaireResultExportRespVO>> targetMap = batchRows.stream()
                    .sorted(Comparator.comparing((QuestionnaireResultExportRespVO r) -> text(r.getTargetName()))
                            .thenComparing(r -> r.getItemSortNo() == null ? Integer.MAX_VALUE : r.getItemSortNo())
                            .thenComparing(r -> text(r.getEvaluatorName())))
                    .collect(Collectors.groupingBy(r -> peerReport
                                    ? text(r.getTargetId()) + "_" + text(r.getTargetName())
                                    : text(r.getAssignmentId()) + "_" + text(r.getEvaluatorName()),
                            LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<QuestionnaireResultExportRespVO>> targetEntry : targetMap.entrySet()) {
                List<QuestionnaireResultExportRespVO> targetRows = targetEntry.getValue();
                if (targetRows == null || targetRows.isEmpty()) {
                    continue;
                }
                QuestionnaireResultExportRespVO firstTargetRow = targetRows.get(0);
                String subjectName = resolveAnswerPackageSubjectName(firstTargetRow);
                Set<Long> submittedAssignmentIds = targetRows.stream()
                        .filter(r -> "已提交".equals(r.getStatusText()))
                        .map(QuestionnaireResultExportRespVO::getAssignmentId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Map<Long, Integer> assignmentTotalMap = targetRows.stream()
                        .filter(r -> r.getAssignmentId() != null)
                        .collect(Collectors.toMap(QuestionnaireResultExportRespVO::getAssignmentId,
                                r -> r.getTotalScore() == null ? 0 : r.getTotalScore(), (l, r) -> l));
                double avgScore = submittedAssignmentIds.isEmpty() ? 0.0 : submittedAssignmentIds.stream()
                        .map(id -> assignmentTotalMap.getOrDefault(id, 0))
                        .mapToInt(Integer::intValue)
                        .average().orElse(0.0);
                String submitCountLabel = peerReport ? "已评人数：" : "提交次数：";
                String submitCountUnit = peerReport ? "人" : "次";

                rowIndex = appendMergedRow(sheet, rowIndex,
                        buildReportTitle(targetRows, subjectName),
                        styles.titleStyle, 30f);
                rowIndex = appendMergedRow(sheet, rowIndex,
                        (peerReport ? "被评价人：" : "填写人：") + subjectName
                                + "       " + submitCountLabel + submittedAssignmentIds.size()
                                + submitCountUnit + "         平均得分：" + formatAvg(avgScore)
                                + buildBatchDisplayText(targetRows),
                        styles.subTitleStyle, 24f);

                Map<Long, List<QuestionnaireResultExportRespVO>> itemMap = targetRows.stream()
                        .filter(r -> r.getItemId() != null)
                        .collect(Collectors.groupingBy(QuestionnaireResultExportRespVO::getItemId));
                List<Long> itemIds = new ArrayList<>(itemMap.keySet());
                itemIds.sort(Comparator.comparing((Long itemId) -> {
                    QuestionnaireResultExportRespVO first = itemMap.get(itemId).get(0);
                    return first.getItemSortNo() == null ? Integer.MAX_VALUE : first.getItemSortNo();
                }).thenComparing(Long::longValue));

                for (Long itemId : itemIds) {
                    List<QuestionnaireResultExportRespVO> itemRows = itemMap.get(itemId);
                    itemRows.sort(Comparator.comparing((QuestionnaireResultExportRespVO r) -> text(r.getEvaluatorName()))
                            .thenComparing(r -> r.getAssignmentId() == null ? Long.MAX_VALUE : r.getAssignmentId()));
                    QuestionnaireResultExportRespVO itemHeader = itemRows.get(0);
                    String questionTitle = (itemHeader.getItemSortNo() == null ? "" : itemHeader.getItemSortNo() + ". ")
                            + text(itemHeader.getItemTitle());
                    int questionScore = itemRows.stream()
                            .filter(r -> "已提交".equals(r.getStatusText()))
                            .map(QuestionnaireResultExportRespVO::getItemScore)
                            .filter(Objects::nonNull)
                            .mapToInt(Integer::intValue)
                            .sum();
                    List<ItemDetailLine> detailLines = buildItemDetailLines(itemRows, optionMap.get(itemId));

                    int questionStartRow = rowIndex;
                    Row qRow = sheet.createRow(rowIndex++);
                    qRow.setHeightInPoints(24f);
                    setCell(qRow, 0, questionTitle, styles.questionStyle);
                    setCell(qRow, 1, "", styles.questionStyle);
                    setCell(qRow, 2, questionScore, styles.scoreStyle);
                    sheet.addMergedRegion(new CellRangeAddress(questionStartRow, questionStartRow, 0, 1));

                    for (ItemDetailLine detailLine : detailLines) {
                        Row detailRow = sheet.createRow(rowIndex++);
                        detailRow.setHeightInPoints(20f);
                        setCell(detailRow, 0, detailLine.getContent(), styles.detailTextStyle);
                        setCell(detailRow, 1, detailLine.getCount(), styles.detailCountStyle);
                        setCell(detailRow, 2, "", styles.scoreStyle);
                    }

                    int questionEndRow = rowIndex - 1;
                    if (questionEndRow > questionStartRow) {
                        sheet.addMergedRegion(new CellRangeAddress(questionStartRow, questionEndRow, 2, 2));
                    }
                }
                rowIndex++;
            }
            if (showBatchTitle && i < batchNos.size() - 1) {
                rowIndex++;
            }
        }
    }

    private boolean isPeerExportRow(QuestionnaireResultExportRespVO row) {
        return row != null && "peer".equalsIgnoreCase(row.getQuestionnaireType());
    }

    private String buildReportTitle(List<QuestionnaireResultExportRespVO> rows, String subjectName) {
        String questionnaireName = resolveQuestionnaireName(rows);
        String prefix = text(subjectName).trim();
        if (prefix.isEmpty()) {
            return questionnaireName + " 答题记录";
        }
        return prefix + " " + questionnaireName + " 答题记录";
    }

    private String resolveQuestionnaireName(List<QuestionnaireResultExportRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return "问卷";
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .map(QuestionnaireResultExportRespVO::getQuestionnaireName)
                .map(this::text)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .findFirst()
                .orElse("问卷");
    }

    private String buildBatchDisplayText(List<QuestionnaireResultExportRespVO> rows) {
        String batchDisplay = resolveBatchDisplay(rows);
        return batchDisplay.isEmpty() ? "" : "         批次：" + batchDisplay;
    }

    private String resolveBatchSuffix(List<QuestionnaireResultExportRespVO> rows) {
        String batchDisplay = resolveBatchDisplay(rows);
        return batchDisplay.isEmpty() ? "" : "_" + batchDisplay;
    }

    private String resolveBatchDisplay(List<QuestionnaireResultExportRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        List<String> batchDisplays = rows.stream()
                .filter(Objects::nonNull)
                .map(this::resolveRowBatchDisplay)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (batchDisplays.isEmpty()) {
            return "";
        }
        if (batchDisplays.size() == 1) {
            return batchDisplays.get(0);
        }
        return "全部批次";
    }

    private String resolveRowBatchDisplay(QuestionnaireResultExportRespVO row) {
        String batchLabel = text(row.getBatchLabel()).trim();
        if (!batchLabel.isEmpty()) {
            return batchLabel;
        }
        Integer batchNo = row.getBatchNo();
        if (batchNo == null || batchNo <= 0) {
            return "未分期";
        }
        return "第" + batchNo + "期";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String formatAvg(double value) {
        return String.format(Locale.CHINA, "%.1f", value);
    }

    private int appendMergedRow(Sheet sheet, int rowIndex, String value, CellStyle style, float height) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(height);
        setCell(row, 0, value, style);
        setCell(row, 1, "", style);
        setCell(row, 2, "", style);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 2));
        return rowIndex + 1;
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

    private Map<Long, List<QuestionnaireOptionDO>> buildOptionMap(List<QuestionnaireResultExportRespVO> list) {
        if (list == null || list.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> itemIds = list.stream().map(QuestionnaireResultExportRespVO::getItemId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (itemIds.isEmpty()) {
            return new HashMap<>();
        }
        return optionMapper.selectListByItemIds(itemIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(QuestionnaireOptionDO::getItemId));
    }

    private List<ItemDetailLine> buildItemDetailLines(List<QuestionnaireResultExportRespVO> itemRows, List<QuestionnaireOptionDO> options) {
        List<QuestionnaireResultExportRespVO> submittedRows = itemRows.stream()
                .filter(r -> "已提交".equals(r.getStatusText()))
                .collect(Collectors.toList());
        Map<String, String> optionAliasMap = buildOptionAliasMap(options);
        Set<String> optionKeySet = optionAliasMap.values().stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .collect(Collectors.toSet());
        if (isMultiItem(itemRows)) {
            return buildMultiItemDetailLines(submittedRows, optionAliasMap, optionKeySet);
        }
        Map<Integer, String> scoreOptionMap = buildUniqueScoreOptionMap(options);
        Map<String, Integer> answerCountMap = new LinkedHashMap<>();
        int emptyAnswerCount = 0;
        for (QuestionnaireResultExportRespVO row : submittedRows) {
            boolean hasMatched = false;
            for (String answer : splitSingleAnswers(row.getAnswerContent())) {
                String key = resolveAnswerKey(answer, optionAliasMap);
                if (key.isEmpty()) {
                    continue;
                }
                if (!optionKeySet.isEmpty() && !optionKeySet.contains(key)) {
                    continue;
                }
                incrementAnswerCount(answerCountMap, key);
                hasMatched = true;
            }
            if (!hasMatched) {
                String scoreKey = resolveOptionByScore(row.getItemScore(), scoreOptionMap);
                if (!scoreKey.isEmpty()) {
                    incrementAnswerCount(answerCountMap, scoreKey);
                    hasMatched = true;
                }
            }
            if (!hasMatched) {
                emptyAnswerCount++;
            }
        }
        List<ItemDetailLine> lines = new ArrayList<>();
        if (options != null && !options.isEmpty()) {
            for (QuestionnaireOptionDO option : options) {
                String optionText = text(option.getOptionText()).trim();
                String display = optionText;
                if (option.getOptionScore() != null) {
                    display = optionText + "（" + option.getOptionScore() + "）";
                }
                lines.add(new ItemDetailLine(display, answerCountMap.getOrDefault(normalizeAnswerKey(optionText), 0)));
            }
            if (emptyAnswerCount > 0) {
                lines.add(new ItemDetailLine("未填写", emptyAnswerCount));
            }
        } else {
            answerCountMap.forEach((answer, count) -> lines.add(new ItemDetailLine(answer, count)));
            if (emptyAnswerCount > 0) {
                lines.add(new ItemDetailLine("未填写", emptyAnswerCount));
            }
        }
        if (lines.isEmpty()) {
            lines.add(new ItemDetailLine("暂无作答", 0));
        }
        return lines;
    }

    private boolean isMultiItem(List<QuestionnaireResultExportRespVO> itemRows) {
        return itemRows.stream().anyMatch(row -> "多选".equals(text(row.getItemTypeText())));
    }

    private List<ItemDetailLine> buildMultiItemDetailLines(List<QuestionnaireResultExportRespVO> submittedRows,
                                                           Map<String, String> optionAliasMap,
                                                           Set<String> optionKeySet) {
        List<ItemDetailLine> lines = new ArrayList<>();
        for (QuestionnaireResultExportRespVO row : submittedRows) {
            LinkedHashSet<String> resolvedAnswers = new LinkedHashSet<>();
            for (String answer : splitMultiAnswers(row.getAnswerContent())) {
                String key = resolveAnswerKey(answer, optionAliasMap);
                if (key.isEmpty()) {
                    continue;
                }
                if (!optionKeySet.isEmpty() && !optionKeySet.contains(key)) {
                    continue;
                }
                resolvedAnswers.add(key);
            }
            String answerText = resolvedAnswers.isEmpty() ? "未填写" : String.join(" / ", resolvedAnswers);
            lines.add(new ItemDetailLine(resolveEvaluatorLabel(row) + "：" + answerText, 1));
        }
        if (lines.isEmpty()) {
            lines.add(new ItemDetailLine("暂无作答", 0));
        }
        return lines;
    }

    private String resolveEvaluatorLabel(QuestionnaireResultExportRespVO row) {
        String evaluatorName = text(row.getEvaluatorName()).trim();
        if (!evaluatorName.isEmpty()) {
            return evaluatorName;
        }
        if (row.getEvaluatorId() != null) {
            return "评价人#" + row.getEvaluatorId();
        }
        return "评价人";
    }

    private void incrementAnswerCount(Map<String, Integer> answerCountMap, String key) {
        answerCountMap.put(key, answerCountMap.getOrDefault(key, 0) + 1);
    }

    private Map<Integer, String> buildUniqueScoreOptionMap(List<QuestionnaireOptionDO> options) {
        Map<Integer, String> scoreMap = new HashMap<>();
        Set<Integer> duplicatedScores = new HashSet<>();
        if (options == null || options.isEmpty()) {
            return scoreMap;
        }
        for (QuestionnaireOptionDO option : options) {
            if (option == null || option.getOptionScore() == null) {
                continue;
            }
            String optionText = normalizeAnswerKey(option.getOptionText());
            if (optionText.isEmpty()) {
                continue;
            }
            Integer score = option.getOptionScore();
            if (duplicatedScores.contains(score)) {
                continue;
            }
            if (scoreMap.containsKey(score) && !Objects.equals(scoreMap.get(score), optionText)) {
                scoreMap.remove(score);
                duplicatedScores.add(score);
                continue;
            }
            scoreMap.put(score, optionText);
        }
        return scoreMap;
    }

    private String resolveOptionByScore(Integer score, Map<Integer, String> scoreOptionMap) {
        if (score == null || scoreOptionMap == null || scoreOptionMap.isEmpty()) {
            return "";
        }
        return text(scoreOptionMap.get(score)).trim();
    }

    private List<String> splitSingleAnswers(String answerContent) {
        String content = text(answerContent).trim();
        if (content.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> parsedJsonAnswers = parseJsonAnswers(content);
        if (!parsedJsonAnswers.isEmpty()) {
            return parsedJsonAnswers;
        }
        String normalized = normalizeAnswerKey(content);
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        result.add(normalized);
        return result;
    }

    private List<String> splitMultiAnswers(String answerContent) {
        String content = text(answerContent).trim();
        if (content.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> parsedJsonAnswers = parseJsonAnswers(content);
        if (!parsedJsonAnswers.isEmpty()) {
            return parsedJsonAnswers;
        }
        String[] parts = content.split("\\s*/\\s*");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String answer = normalizeAnswerKey(part);
            if (!answer.isEmpty()) {
                result.add(answer);
            }
        }
        return result.isEmpty() ? new ArrayList<>() : result;
    }

    private List<String> parseJsonAnswers(String content) {
        if (!JsonUtils.isJson(content)) {
            return new ArrayList<>();
        }
        try {
            Object parsed = JsonUtils.parseObject(content, Object.class);
            List<String> values = new ArrayList<>();
            flattenJsonAnswerValue(parsed, values);
            return values.stream()
                    .map(this::normalizeAnswerKey)
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private void flattenJsonAnswerValue(Object value, List<String> collector) {
        if (value == null) {
            return;
        }
        if (value instanceof List) {
            for (Object item : (List<Object>) value) {
                flattenJsonAnswerValue(item, collector);
            }
            return;
        }
        if (value instanceof Map) {
            for (Object mapValue : ((Map<Object, Object>) value).values()) {
                flattenJsonAnswerValue(mapValue, collector);
            }
            return;
        }
        collector.add(String.valueOf(value));
    }

    private String normalizeAnswerKey(String answer) {
        String value = text(answer).trim();
        if (value.isEmpty()) {
            return value;
        }
        value = value.replaceAll("^[\\s\"'“”‘’\\[\\](){}]+|[\\s\"'“”‘’\\[\\](){}]+$", "").trim();
        value = value.replaceAll("[（(]\\s*\\d+\\s*[）)]\\s*$", "").trim();
        return "null".equalsIgnoreCase(value) ? "" : value;
    }

    private Map<String, String> buildOptionAliasMap(List<QuestionnaireOptionDO> options) {
        Map<String, String> aliasMap = new HashMap<>();
        if (options == null || options.isEmpty()) {
            return aliasMap;
        }
        for (QuestionnaireOptionDO option : options) {
            String optionText = normalizeAnswerKey(option.getOptionText());
            if (optionText.isEmpty()) {
                continue;
            }
            aliasMap.putIfAbsent(optionText, optionText);
            if (option.getId() != null) {
                aliasMap.putIfAbsent(String.valueOf(option.getId()), optionText);
            }
        }
        return aliasMap;
    }

    private String resolveAnswerKey(String answer, Map<String, String> optionAliasMap) {
        String normalized = normalizeAnswerKey(answer);
        if (normalized.isEmpty()) {
            return normalized;
        }
        String mapped = optionAliasMap.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        for (Map.Entry<String, String> entry : optionAliasMap.entrySet()) {
            String alias = text(entry.getKey()).trim();
            if (alias.isEmpty()) {
                continue;
            }
            if (normalized.contains(alias) || alias.contains(normalized)) {
                return entry.getValue();
            }
        }
        return normalized;
    }

    private ReportStyles buildStyles(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setFontName("Calibri");
        titleFont.setFontHeightInPoints((short) 20);

        Font subTitleFont = workbook.createFont();
        subTitleFont.setFontName("Calibri");
        subTitleFont.setFontHeightInPoints((short) 14);

        Font bodyFont = workbook.createFont();
        bodyFont.setFontName("宋体");
        bodyFont.setFontHeightInPoints((short) 12);

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(titleStyle);

        CellStyle subTitleStyle = workbook.createCellStyle();
        subTitleStyle.setFont(subTitleFont);
        subTitleStyle.setAlignment(HorizontalAlignment.LEFT);
        subTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(subTitleStyle);

        CellStyle batchTitleStyle = workbook.createCellStyle();
        batchTitleStyle.setFont(bodyFont);
        batchTitleStyle.setAlignment(HorizontalAlignment.LEFT);
        batchTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        batchTitleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        batchTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setThinBorder(batchTitleStyle);

        CellStyle questionStyle = workbook.createCellStyle();
        questionStyle.setFont(bodyFont);
        questionStyle.setAlignment(HorizontalAlignment.LEFT);
        questionStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        questionStyle.setWrapText(false);
        questionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        if (questionStyle instanceof XSSFCellStyle) {
            ((XSSFCellStyle) questionStyle).setFillForegroundColor(new XSSFColor(new Color(226, 239, 218), null));
        }
        setThinBorder(questionStyle);

        CellStyle detailTextStyle = workbook.createCellStyle();
        detailTextStyle.setFont(bodyFont);
        detailTextStyle.setAlignment(HorizontalAlignment.LEFT);
        detailTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        detailTextStyle.setWrapText(false);
        setThinBorder(detailTextStyle);

        CellStyle detailCountStyle = workbook.createCellStyle();
        detailCountStyle.setFont(bodyFont);
        detailCountStyle.setAlignment(HorizontalAlignment.CENTER);
        detailCountStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(detailCountStyle);

        CellStyle scoreStyle = workbook.createCellStyle();
        scoreStyle.setFont(bodyFont);
        scoreStyle.setAlignment(HorizontalAlignment.CENTER);
        scoreStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(scoreStyle);

        return new ReportStyles(titleStyle, subTitleStyle, batchTitleStyle, questionStyle,
                detailTextStyle, detailCountStyle, scoreStyle);
    }

    private void setThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private AnswerDetailStyles buildAnswerDetailStyles(Workbook workbook) {
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

        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setFont(bodyFont);
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        centerStyle.setWrapText(true);
        setThinBorder(centerStyle);

        return new AnswerDetailStyles(titleStyle, summaryStyle, headerStyle, bodyStyle, centerStyle);
    }

    private SummaryStyles buildSummaryStyles(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setFontName("宋体");
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);

        Font bodyFont = workbook.createFont();
        bodyFont.setFontName("宋体");
        bodyFont.setFontHeightInPoints((short) 11);

        Font linkFont = workbook.createFont();
        linkFont.setFontName("宋体");
        linkFont.setFontHeightInPoints((short) 11);
        linkFont.setColor(IndexedColors.BLUE.getIndex());
        linkFont.setUnderline(Font.U_SINGLE);

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
        setThinBorder(bodyStyle);

        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setFont(bodyFont);
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setThinBorder(centerStyle);

        CellStyle linkStyle = workbook.createCellStyle();
        linkStyle.cloneStyleFrom(bodyStyle);
        linkStyle.setFont(linkFont);

        return new SummaryStyles(titleStyle, summaryStyle, headerStyle, bodyStyle, centerStyle, linkStyle);
    }

    private static class ReportSheetGroup {
        private final String subjectName;
        private final String sheetName;
        private final List<QuestionnaireResultExportRespVO> rows;

        private ReportSheetGroup(String subjectName, String sheetName, List<QuestionnaireResultExportRespVO> rows) {
            this.subjectName = subjectName;
            this.sheetName = sheetName;
            this.rows = rows;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getSheetName() {
            return sheetName;
        }

        public List<QuestionnaireResultExportRespVO> getRows() {
            return rows;
        }
    }

    private static class ReportSubjectStats {
        private final int expectedCount;
        private final int submittedCount;
        private final int pendingCount;
        private final double averageScore;

        private ReportSubjectStats(int expectedCount, int submittedCount, int pendingCount, double averageScore) {
            this.expectedCount = expectedCount;
            this.submittedCount = submittedCount;
            this.pendingCount = pendingCount;
            this.averageScore = averageScore;
        }

        public int getExpectedCount() {
            return expectedCount;
        }

        public int getSubmittedCount() {
            return submittedCount;
        }

        public int getPendingCount() {
            return pendingCount;
        }

        public double getAverageScore() {
            return averageScore;
        }
    }

    private static class SummaryStyles {
        private final CellStyle titleStyle;
        private final CellStyle summaryStyle;
        private final CellStyle headerStyle;
        private final CellStyle bodyStyle;
        private final CellStyle centerStyle;
        private final CellStyle linkStyle;

        private SummaryStyles(CellStyle titleStyle, CellStyle summaryStyle, CellStyle headerStyle,
                              CellStyle bodyStyle, CellStyle centerStyle, CellStyle linkStyle) {
            this.titleStyle = titleStyle;
            this.summaryStyle = summaryStyle;
            this.headerStyle = headerStyle;
            this.bodyStyle = bodyStyle;
            this.centerStyle = centerStyle;
            this.linkStyle = linkStyle;
        }
    }

    private static class ItemDetailLine {
        private final String content;
        private final int count;

        private ItemDetailLine(String content, int count) {
            this.content = content;
            this.count = count;
        }

        public String getContent() {
            return content;
        }

        public int getCount() {
            return count;
        }
    }

    private static class ReportStyles {
        private final CellStyle titleStyle;
        private final CellStyle subTitleStyle;
        private final CellStyle batchTitleStyle;
        private final CellStyle questionStyle;
        private final CellStyle detailTextStyle;
        private final CellStyle detailCountStyle;
        private final CellStyle scoreStyle;

        private ReportStyles(CellStyle titleStyle, CellStyle subTitleStyle, CellStyle batchTitleStyle,
                             CellStyle questionStyle, CellStyle detailTextStyle, CellStyle detailCountStyle,
                             CellStyle scoreStyle) {
            this.titleStyle = titleStyle;
            this.subTitleStyle = subTitleStyle;
            this.batchTitleStyle = batchTitleStyle;
            this.questionStyle = questionStyle;
            this.detailTextStyle = detailTextStyle;
            this.detailCountStyle = detailCountStyle;
            this.scoreStyle = scoreStyle;
        }
    }

    private static class AnswerDetailStyles {
        private final CellStyle titleStyle;
        private final CellStyle summaryStyle;
        private final CellStyle headerStyle;
        private final CellStyle bodyStyle;
        private final CellStyle centerStyle;

        private AnswerDetailStyles(CellStyle titleStyle, CellStyle summaryStyle, CellStyle headerStyle,
                                   CellStyle bodyStyle, CellStyle centerStyle) {
            this.titleStyle = titleStyle;
            this.summaryStyle = summaryStyle;
            this.headerStyle = headerStyle;
            this.bodyStyle = bodyStyle;
            this.centerStyle = centerStyle;
        }
    }

    @GetMapping("/item-stats")
    @Operation(summary = "获得问卷题目统计")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:result:query')")
    public CommonResult<List<QuestionnaireItemStatRespVO>> getItemStats(
            @RequestParam("publishId") Long publishId,
            @RequestParam("questionnaireId") Long questionnaireId) {
        return success(resultService.getItemStats(publishId, questionnaireId));
    }

    @GetMapping("/item-stats-export")
    @Operation(summary = "导出问卷题目统计")
    @PreAuthorize("@ss.hasPermission('hr:questionnaire:result:export')")
    public void exportItemStats(
            @RequestParam("publishId") Long publishId,
            @RequestParam("questionnaireId") Long questionnaireId,
            HttpServletResponse response)
            throws IOException {
        List<QuestionnaireItemStatRespVO> list = resultService.getItemStats(publishId, questionnaireId);
        ExcelUtils.write(response, "问卷题目统计.xls", "数据", QuestionnaireItemStatRespVO.class, list);
    }

}
