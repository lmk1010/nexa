package com.kyx.service.erp.controller.admin.purchase.vo.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购申请分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseRequestPageReqVO extends PageParam {

    @Schema(description = "申请单号", example = "PR202412010001")
    private String requestNo;

    @Schema(description = "申请标题", example = "办公用品采购申请")
    private String title;

    @Schema(description = "申请人", example = "张三")
    private String applicant;

    @Schema(description = "申请部门", example = "行政部")
    private String department;

    @Schema(description = "联系电话", example = "13812345678")
    private String contactPhone;

    @Schema(description = "预算科目", example = "办公用品采购")
    private String budgetAccount;

    @Schema(description = "状态：0-草稿，1-待审核，2-已通过，3-已拒绝，4-已取消", example = "1")
    private Integer status;

    @Schema(description = "紧急程度：1-低，2-一般，3-高，4-紧急", example = "2")
    private Integer urgentLevel;

    @Schema(description = "申请日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date[] applyDate;

    @Schema(description = "需求日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date[] requiredDate;

    @Schema(description = "总金额（最小值）", example = "100.00")
    private BigDecimal totalAmountMin;

    @Schema(description = "总金额（最大值）", example = "10000.00")
    private BigDecimal totalAmountMax;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private Date[] createTime;

} 