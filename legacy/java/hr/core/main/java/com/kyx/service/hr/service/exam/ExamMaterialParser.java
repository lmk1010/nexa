package com.kyx.service.hr.service.exam;

import com.kyx.service.hr.controller.admin.exam.vo.ExamMaterialParseRespVO;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 培训资料解析器，仅支持 Word / Excel。
 */
public class ExamMaterialParser {

    public static ExamMaterialParseRespVO parse(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        String lowerName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        String content;
        try (InputStream inputStream = file.getInputStream()) {
            if (lowerName.endsWith(".docx")) {
                content = parseDocx(inputStream);
            } else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                content = parseExcel(inputStream);
            } else {
                throw new IllegalArgumentException("unsupported material type");
            }
        }
        ExamMaterialParseRespVO respVO = new ExamMaterialParseRespVO();
        respVO.setFileName(fileName);
        respVO.setContent(content);
        return respVO;
    }

    private static String parseDocx(InputStream inputStream) throws Exception {
        List<String> lines = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                appendLine(lines, paragraph.getText());
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell == null ? "" : cell.getText();
                        text = normalizeText(text);
                        if (!text.isEmpty()) {
                            cells.add(text);
                        }
                    }
                    if (!cells.isEmpty()) {
                        lines.add(String.join(" | ", cells));
                    }
                }
            }
        }
        return String.join("\n", lines).trim();
    }

    private static String parseExcel(InputStream inputStream) throws Exception {
        List<String> lines = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (sheet == null) {
                    continue;
                }
                appendLine(lines, "工作表：" + sheet.getSheetName());
                for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    List<String> cells = new ArrayList<>();
                    for (int cellIndex = row.getFirstCellNum(); cellIndex >= 0 && cellIndex < row.getLastCellNum(); cellIndex++) {
                        String text = normalizeText(formatter.formatCellValue(row.getCell(cellIndex)));
                        if (!text.isEmpty()) {
                            cells.add(text);
                        }
                    }
                    if (!cells.isEmpty()) {
                        lines.add(String.join(" | ", cells));
                    }
                }
            }
        }
        return String.join("\n", lines).trim();
    }

    private static void appendLine(List<String> lines, String text) {
        String normalized = normalizeText(text);
        if (!normalized.isEmpty()) {
            lines.add(normalized);
        }
    }

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u00A0', ' ')
                .replace("\r", "\n")
                .replaceAll("\\n{2,}", "\n")
                .trim();
    }

}
