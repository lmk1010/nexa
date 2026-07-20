package com.kyx.service.hr.service.exam;

import com.kyx.foundation.common.util.json.JsonUtils;
import com.kyx.service.hr.controller.admin.exam.vo.ExamImportRespVO;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExamExcelParser {

    public static ExamImportRespVO parse(InputStream inputStream, String fileName) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.CHINA);

            List<ExamImportRespVO.Item> items = new ArrayList<>();
            int sortNo = 1;
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String title = formatter.formatCellValue(row.getCell(0)).trim();
                if (title.isEmpty()) {
                    continue;
                }
                if (i == 0 && ("题干".equals(title) || title.contains("题干"))) {
                    continue;
                }
                String typeRaw = formatter.formatCellValue(row.getCell(1)).trim();
                String optionsRaw = formatter.formatCellValue(row.getCell(2)).trim();
                String answerRaw = formatter.formatCellValue(row.getCell(3)).trim();
                String scoreRaw = formatter.formatCellValue(row.getCell(4)).trim();
                String analysisRaw = formatter.formatCellValue(row.getCell(5)).trim();

                ExamImportRespVO.Item item = new ExamImportRespVO.Item();
                item.setTitle(title);
                item.setItemType(mapType(typeRaw));
                item.setRequired(true);
                item.setSortNo(sortNo++);
                item.setScore(parseInt(scoreRaw));

                List<String> options = parseOptions(optionsRaw, item.getItemType());
                if (!options.isEmpty()) {
                    List<ExamImportRespVO.Option> optionList = new ArrayList<>();
                    List<String> answerList = parseAnswerList(answerRaw, item.getItemType());
                    for (String opt : options) {
                        ExamImportRespVO.Option option = new ExamImportRespVO.Option();
                        option.setText(opt);
                        option.setIsCorrect(answerList.contains(opt));
                        optionList.add(option);
                    }
                    item.setOptions(optionList);
                    item.setOptionsJson(JsonUtils.toJsonString(optionList));
                }
                if (!answerRaw.isEmpty() || !analysisRaw.isEmpty()) {
                    item.setAnswerJson(buildAnswerPayload(answerRaw, item.getItemType(), analysisRaw));
                }
                items.add(item);
            }

            ExamImportRespVO.Paper paper = new ExamImportRespVO.Paper();
            paper.setName(trimExt(fileName));
            paper.setItems(items);

            ExamImportRespVO resp = new ExamImportRespVO();
            resp.setPapers(Collections.singletonList(paper));
            return resp;
        }
    }

    private static String mapType(String typeRaw) {
        String raw = typeRaw == null ? "" : typeRaw.trim().toLowerCase();
        if (raw.contains("单选") || "single".equals(raw)) {
            return "single";
        }
        if (raw.contains("多选") || "multi".equals(raw)) {
            return "multi";
        }
        if (raw.contains("判断") || "judge".equals(raw)) {
            return "judge";
        }
        if (raw.contains("填空") || "blank".equals(raw)) {
            return "blank";
        }
        if (raw.contains("简答") || "short".equals(raw)) {
            return "short";
        }
        return "single";
    }

    private static List<String> parseOptions(String optionsRaw, String type) {
        if (optionsRaw == null || optionsRaw.trim().isEmpty()) {
            if ("judge".equals(type)) {
                List<String> defaults = new ArrayList<>();
                defaults.add("正确");
                defaults.add("错误");
                return defaults;
            }
            return Collections.emptyList();
        }
        String raw = optionsRaw.replace("；", "|").replace("，", "|");
        String[] parts = raw.split("\\|");
        List<String> list = new ArrayList<>();
        for (String part : parts) {
            String val = part.trim();
            if (!val.isEmpty()) {
                list.add(val);
            }
        }
        return list;
    }

    private static List<String> parseAnswerList(String answerRaw, String type) {
        if (answerRaw == null || answerRaw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if ("multi".equals(type)) {
            String raw = answerRaw.replace("；", "|").replace("，", "|");
            String[] parts = raw.split("\\|");
            List<String> list = new ArrayList<>();
            for (String part : parts) {
                String val = part.trim();
                if (!val.isEmpty()) {
                    list.add(val);
                }
            }
            return list;
        }
        return Collections.singletonList(answerRaw.trim());
    }

    private static String buildAnswerJson(String answerRaw, String type) {
        if ("multi".equals(type)) {
            return JsonUtils.toJsonString(parseAnswerList(answerRaw, type));
        }
        return JsonUtils.toJsonString(answerRaw.trim());
    }

    private static String buildAnswerPayload(String answerRaw, String type, String analysisRaw) {
        String analysis = analysisRaw == null ? "" : analysisRaw.trim();
        if (analysis.isEmpty()) {
            return buildAnswerJson(answerRaw, type);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        if (answerRaw != null && !answerRaw.trim().isEmpty()) {
            payload.put("value", "multi".equals(type)
                    ? parseAnswerList(answerRaw, type)
                    : answerRaw.trim());
        }
        payload.put("analysis", analysis);
        return JsonUtils.toJsonString(payload);
    }

    private static Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private static String trimExt(String fileName) {
        if (fileName == null) {
            return "考试试卷";
        }
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }
}
