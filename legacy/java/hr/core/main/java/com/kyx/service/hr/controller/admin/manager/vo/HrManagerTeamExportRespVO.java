package com.kyx.service.hr.controller.admin.manager.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class HrManagerTeamExportRespVO {

    @ExcelProperty("员工档案ID")
    private Long profileId;

    @ExcelProperty("员工编号")
    private String employeeNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("岗位")
    private String jobTitle;

    @ExcelProperty("工作状态")
    private String workStatusText;

    @ExcelProperty("用工类型")
    private String employmentTypeText;

    @ExcelProperty("入职日期")
    private LocalDate entryDate;

    @ExcelProperty("合同到期")
    private LocalDate contractEndDate;

    @ExcelProperty("最新绩效周期")
    private String latestPerformancePeriod;

    @ExcelProperty("最新绩效等级")
    private String latestPerformanceGrade;

    @ExcelProperty("最新绩效结果")
    private String latestPerformanceResult;

    @ExcelProperty("最新绩效得分")
    private BigDecimal latestPerformanceScore;

    @ExcelProperty("开放待办")
    private Integer openTodoCount;

    @ExcelProperty("考勤异常")
    private Integer attendanceExceptionCount;

    @ExcelProperty("更正待审")
    private Integer pendingCorrectionCount;

    @ExcelProperty("材料到期")
    private Integer materialExpiringCount;

    @ExcelProperty("试用到期")
    private LocalDate probationDueDate;

    @ExcelProperty("培训完成")
    private Integer trainingCompletedCount;

    @ExcelProperty("培训任务")
    private Integer trainingAssignmentCount;

    @ExcelProperty("风险等级")
    private String riskLevel;

    @ExcelProperty("风险说明")
    private String riskReason;

}
