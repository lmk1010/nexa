package com.kyx.service.hr.controller.admin.payroll.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayslipExportRespVO {

    @ExcelProperty("工资条ID")
    private Long id;

    @ExcelProperty("工资月份")
    private String payrollMonth;

    @ExcelProperty("员工档案ID")
    private Long profileId;

    @ExcelProperty("员工姓名")
    private String profileName;

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("用户昵称")
    private String userNickname;

    @ExcelProperty("币种")
    private String currency;

    @ExcelProperty("基本工资")
    private BigDecimal baseSalary;

    @ExcelProperty("考勤扣减")
    private BigDecimal attendanceDeduction;

    @ExcelProperty("加班工资")
    private BigDecimal overtimePay;

    @ExcelProperty("奖金")
    private BigDecimal bonus;

    @ExcelProperty("津贴")
    private BigDecimal allowance;

    @ExcelProperty("其他扣减")
    private BigDecimal deduction;

    @ExcelProperty("社保")
    private BigDecimal socialInsurance;

    @ExcelProperty("公积金")
    private BigDecimal housingFund;

    @ExcelProperty("个税")
    private BigDecimal tax;

    @ExcelProperty("实发工资")
    private BigDecimal netSalary;

    @ExcelProperty("状态")
    private String statusName;

    @ExcelProperty("确认时间")
    private LocalDateTime confirmedTime;

    @ExcelProperty("异议时间")
    private LocalDateTime issueTime;

    @ExcelProperty("异议说明")
    private String issueRemark;

    @ExcelProperty("处理人")
    private String resolvedByName;

    @ExcelProperty("处理时间")
    private LocalDateTime resolvedTime;

    @ExcelProperty("处理说明")
    private String resolveRemark;

}
