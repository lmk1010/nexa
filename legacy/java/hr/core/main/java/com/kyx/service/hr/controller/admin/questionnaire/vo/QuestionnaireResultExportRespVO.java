package com.kyx.service.hr.controller.admin.questionnaire.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.util.Date;

/**
 * 问卷结果导出明细 Response VO
 *
 * @author MK
 */
@Data
@ExcelIgnoreUnannotated
public class QuestionnaireResultExportRespVO {

    @ExcelProperty("批次")
    private Integer batchNo;

    @ExcelProperty("批次标签")
    private String batchLabel;

    @ExcelProperty("问卷名称")
    private String questionnaireName;

    private String questionnaireType;

    @ExcelProperty("分配ID")
    private Long assignmentId;

    @ExcelProperty("评价人ID")
    private Long evaluatorId;

    @ExcelProperty("评价人")
    private String evaluatorName;

    @ExcelProperty("被评人ID")
    private Long targetId;

    @ExcelProperty("被评人")
    private String targetName;

    @ExcelProperty("状态")
    private String statusText;

    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("提交时间")
    private Date submitTime;

    @ExcelProperty("题目ID")
    private Long itemId;

    @ExcelProperty("题号")
    private Integer itemSortNo;

    @ExcelProperty("题目")
    private String itemTitle;

    @ExcelProperty("题型")
    private String itemTypeText;

    @ExcelProperty("作答内容")
    private String answerContent;

    @ExcelProperty("该题得分")
    private Integer itemScore;

    @ExcelProperty("总分")
    private Integer totalScore;

    @ExcelProperty("总题数")
    private Integer totalItemCount;

}
