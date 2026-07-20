package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 盘点计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpInventoryPlanPageReqVO extends PageParam {

    @Schema(description = "计划名称", example = "2024年第一季度盘点")
    private String planName;

    @Schema(description = "计划编号", example = "INV-2024-001")
    private String planNo;

    @Schema(description = "盘点周期", example = "quarterly")
    private String planType;

    @Schema(description = "盘点方式", example = "full")
    private String method;

    @Schema(description = "盘点计划状态", example = "1")
    private Integer status;

    @Schema(description = "负责人用户ID", example = "1")
    private Long responsiblePersonId;

    @Schema(description = "计划开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] startTime;

    @Schema(description = "计划结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] endTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 