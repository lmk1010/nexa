package com.kyx.service.erp.controller.admin.asset.vo.redistribution;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 资产调拨分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAssetRedistributionPageReqVO extends PageParam {

    @Schema(description = "调拨编号", example = "RD202312010001")
    private String redistributionNo;

    @Schema(description = "调拨前部门ID", example = "1")
    private Long fromDeptId;

    @Schema(description = "调拨到部门ID", example = "2")
    private Long toDeptId;

    @Schema(description = "调拨状态", example = "1")
    private Integer status;

    @Schema(description = "审批状态", example = "1")
    private Integer approvalStatus;

    @Schema(description = "调拨日期开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime allocationDateStart;

    @Schema(description = "调拨日期结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime allocationDateEnd;

    @Schema(description = "创建时间开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeEnd;
} 