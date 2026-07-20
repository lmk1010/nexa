package com.kyx.service.hr.service.questionnaire;

import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireImportRespVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireItemSaveReqVO;
import com.kyx.service.hr.controller.admin.questionnaire.vo.QuestionnaireOptionSaveReqVO;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestionnaireExcelParser {

    private static final Pattern QUESTION_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*[\\.、．]\\s*(.*)$");
    private static final Pattern OPTION_SCORE_PATTERN = Pattern.compile("（\\s*(\\d+)\\s*）|\\(\\s*(\\d+)\\s*\\)");
    private static final Pattern SCORE_IN_TITLE_PATTERN = Pattern.compile("满分\\s*(\\d+)\\s*分|（\\s*(\\d+)\\s*分\\s*）|\\(\\s*(\\d+)\\s*分\\s*\\)");

    public static QuestionnaireImportRespVO parse(InputStream inputStream, String fileName) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.CHINA);

            String name = null;
            List<QuestionnaireItemSaveReqVO> items = new ArrayList<>();

            TempItem current = null;
            int sortNo = 1;
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String cell0 = formatter.formatCellValue(row.getCell(0));
                if (cell0 == null) {
                    continue;
                }
                String text = cell0.trim();
                if (text.isEmpty()) {
                    continue;
                }
                if (name == null) {
                    name = text;
                }

                Matcher qMatcher = QUESTION_PATTERN.matcher(text);
                if (qMatcher.find()) {
                    if (current != null) {
                        items.add(current.toItem(sortNo++));
                    }
                    current = new TempItem();
                    current.rawTitle = text;
                    current.title = qMatcher.group(2).trim();
                    current.required = text.contains("必填");
                    continue;
                }

                if (current != null) {
                    Matcher optMatcher = OPTION_SCORE_PATTERN.matcher(text);
                    if (optMatcher.find()) {
                        String scoreStr = optMatcher.group(1) != null ? optMatcher.group(1) : optMatcher.group(2);
                        Integer score = scoreStr != null ? Integer.valueOf(scoreStr) : null;
                        String optionText = text.replaceAll("（\\s*\\d+\\s*）", "").replaceAll("\\(\\s*\\d+\\s*\\)", "").trim();
                        current.addOption(optionText, score);
                    }
                }
            }
            if (current != null) {
                items.add(current.toItem(sortNo));
            }

            QuestionnaireImportRespVO resp = new QuestionnaireImportRespVO();
            resp.setName(name != null ? name : trimExt(fileName));
            resp.setType(resolveType(resp.getName()));
            resp.setItems(items);
            return resp;
        }
    }

    private static String resolveType(String name) {
        if (name == null) {
            return "peer";
        }
        if (name.contains("考试") || name.contains("测试") || name.contains("考核")) {
            return "exam";
        }
        if (name.contains("印象")) {
            return "employee_impression";
        }
        if (name.contains("互评")) {
            return "peer";
        }
        return "peer";
    }

    private static String trimExt(String fileName) {
        if (fileName == null) {
            return "问卷导入";
        }
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    private static class TempItem {
        String rawTitle;
        String title;
        boolean required;
        final List<QuestionnaireOptionSaveReqVO> options = new ArrayList<>();

        void addOption(String text, Integer score) {
            QuestionnaireOptionSaveReqVO option = new QuestionnaireOptionSaveReqVO();
            option.setOptionText(text);
            option.setOptionScore(score);
            option.setSortNo(options.size() + 1);
            options.add(option);
        }

        QuestionnaireItemSaveReqVO toItem(int sortNo) {
            QuestionnaireItemSaveReqVO item = new QuestionnaireItemSaveReqVO();
            item.setTitle(cleanTitle(title));
            item.setRequired(required);
            item.setSortNo(sortNo);
            if (!options.isEmpty()) {
                item.setItemType(isMulti(rawTitle) ? "multi" : "single");
                item.setOptions(options);
            } else if (isScore(rawTitle)) {
                item.setItemType("score");
                item.setMaxScore(parseMaxScore(rawTitle));
            } else if (isBlank(rawTitle)) {
                item.setItemType("blank");
            } else {
                item.setItemType("text");
            }
            return item;
        }

        private boolean isMulti(String text) {
            return text != null && (text.contains("多选") || text.contains("可多选"));
        }

        private boolean isScore(String text) {
            if (text == null) {
                return false;
            }
            return text.contains("打分") || text.contains("评分") || text.contains("满分");
        }

        private boolean isBlank(String text) {
            if (text == null) {
                return false;
            }
            return text.contains("填空") || text.contains("空格");
        }

        private Integer parseMaxScore(String text) {
            if (text == null) {
                return null;
            }
            Matcher matcher = SCORE_IN_TITLE_PATTERN.matcher(text);
            if (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String val = matcher.group(i);
                    if (val != null) {
                        return Integer.valueOf(val);
                    }
                }
            }
            return null;
        }

        private String cleanTitle(String text) {
            if (text == null) {
                return "";
            }
            return text.replace("（必填）", "").replace("(必填)", "").trim();
        }
    }
}
