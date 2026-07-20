package com.kyx.service.hr.service.exam;

import com.kyx.service.hr.controller.admin.exam.vo.ExamImportRespVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 考试 docx 解析器
 *
 * @author MK
 */
@Slf4j
public class ExamDocxParser {

    private static final Pattern OPTION_PATTERN = Pattern.compile("^([A-H])\\s*[\\.:：；、]?\\s*(.+)$");
    private static final Pattern ANSWER_PATTERN = Pattern.compile("[（(]([A-H]+)[)）]");
    private static final Pattern SCORE_PATTERN = Pattern.compile("[（(](\\d+)\\s*分?[)）]");

    public static ExamImportRespVO parse(InputStream inputStream) {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            List<String> lines = new ArrayList<>();
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText();
                if (text != null) {
                    text = text.trim();
                    if (!text.isEmpty()) {
                        lines.add(text);
                    }
                }
            }

            String sectionType = null;
            String paperTitle = null;
            List<ExamImportRespVO.Item> items = new ArrayList<>();
            ExamImportRespVO.Item current = null;
            List<String> currentAnswerLetters = new ArrayList<>();

            for (String line : lines) {
                if (paperTitle == null && line.contains("考试")) {
                    paperTitle = line;
                    continue;
                }
                if (line.contains("姓名") && line.contains("得分")) {
                    continue;
                }
                if (line.matches("^[一二三四五六七八九十]+、.*")) {
                    if (line.contains("多选题")) sectionType = "multi";
                    else if (line.contains("单选题")) sectionType = "single";
                    else if (line.contains("判断题")) sectionType = "judge";
                    else if (line.contains("填空题")) sectionType = "blank";
                    else if (line.contains("简答题") || line.contains("问答")) sectionType = "short";
                    continue;
                }

                Matcher optionMatcher = OPTION_PATTERN.matcher(line);
                if (optionMatcher.find()) {
                    if (current != null) {
                        ExamImportRespVO.Option opt = new ExamImportRespVO.Option();
                        opt.setText(optionMatcher.group(2));
                        opt.setIsCorrect(currentAnswerLetters.contains(optionMatcher.group(1)));
                        current.getOptions().add(opt);
                    }
                    continue;
                }

                // 新题目
                if (current != null) {
                    items.add(current);
                }
                currentAnswerLetters.clear();
                current = new ExamImportRespVO.Item();
                current.setTitle(line);
                current.setRequired(true);
                current.setSortNo(items.size() + 1);
                current.setOptions(new ArrayList<>());

                // 解析答案字母
                Matcher answerMatcher = ANSWER_PATTERN.matcher(line);
                if (answerMatcher.find()) {
                    String letters = answerMatcher.group(1);
                    for (char c : letters.toCharArray()) {
                        currentAnswerLetters.add(String.valueOf(c));
                    }
                    current.setItemType(letters.length() > 1 ? "multi" : "single");
                    current.setAnswerJson(letters.length() > 1 ? String.format("[\"%s\"]", String.join("\",\"", currentAnswerLetters))
                            : String.format("\"%s\"", letters));
                }

                // 解析分值
                Matcher scoreMatcher = SCORE_PATTERN.matcher(line);
                if (scoreMatcher.find()) {
                    current.setScore(Integer.parseInt(scoreMatcher.group(1)));
                }

                if (current.getItemType() == null && sectionType != null) {
                    current.setItemType(sectionType);
                }
                if (current.getItemType() == null) {
                    current.setItemType("short");
                }
            }

            if (current != null) {
                items.add(current);
            }

            ExamImportRespVO.Paper paper = new ExamImportRespVO.Paper();
            paper.setName(paperTitle != null ? paperTitle : "试卷");
            paper.setItems(items);

            ExamImportRespVO resp = new ExamImportRespVO();
            resp.setPapers(java.util.Collections.singletonList(paper));
            return resp;
        } catch (Exception e) {
            log.error("解析考试 docx 失败", e);
            return null;
        }
    }
}
