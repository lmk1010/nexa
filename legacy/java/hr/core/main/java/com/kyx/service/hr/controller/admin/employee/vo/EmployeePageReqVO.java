package com.kyx.service.hr.controller.admin.employee.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 员工花名册分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EmployeePageReqVO extends PageParam {

    @Schema(description = "员工编号")
    private String employeeNo;

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别（1男 2女）")
    private Integer gender;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "部门ID集合")
    private List<Long> deptIds;

    @Schema(description = "职位")
    private String jobTitle;

    @Schema(description = "员工状态（1正常 0停用）")
    private Integer status;

    @Schema(description = "入职日期开始")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate onboardDateStart;

    @Schema(description = "入职日期结束")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate onboardDateEnd;

    @Schema(description = "工作状态（1待入职 2试用期 3在职 4离职）")
    private Integer workStatus;

    @Schema(description = "排除的工作状态（例如通讯录默认排除 4=离职）")
    private Integer excludeWorkStatus;

    @Schema(description = "用工类型（1全职 2兼职 3劳务 4实习）")
    private Integer employmentType;
} 
