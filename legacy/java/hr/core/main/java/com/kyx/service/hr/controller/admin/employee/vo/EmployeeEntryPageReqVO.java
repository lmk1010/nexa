package com.kyx.service.hr.controller.admin.employee.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 员工入职记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EmployeeEntryPageReqVO extends PageParam {

    @Schema(description = "入职编号")
    private String entryNo;

    @Schema(description = "员工编号")
    private String employeeNo;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "入职类型（1首次入职 2再入职）")
    private Integer entryType;

    @Schema(description = "入职流程类型（1简易入职 2审批入职）")
    private Integer processType;

    @Schema(description = "入职日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate entryDate;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "职位")
    private String jobTitle;

    @Schema(description = "用工类型（1全职 2兼职 3劳务 4实习）")
    private Integer employmentType;

    @Schema(description = "合同类型（1劳动合同 2劳务合同 3实习协议）")
    private Integer contractType;

    @Schema(description = "工作状态（0待填写 1待入职 2试用期 3在职 4离职）")
    private Integer workStatus;

    @Schema(description = "入职状态（1待提交 2审批中 3已通过 4已拒绝 5已取消）")
    private Integer onboardingStatus;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "账户名称")
    private String accountName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] createTime;
} 