package com.kyx.service.erp.controller.admin.asset.vo.assetreturn;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 资产归还记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAssetReturnPageReqVO extends PageParam {

    @Schema(description = "领用记录编号", example = "1")
    private Long checkoutId;

    @Schema(description = "资产编号", example = "1")
    private Long assetId;

    @Schema(description = "归还人编号", example = "1")
    private Long returnUserId;

    @Schema(description = "归还部门编号", example = "1")
    private Long returnDeptId;

    @Schema(description = "归还状态", example = "1")
    private Integer status;

    @Schema(description = "BMP流程状态", example = "1")
    private Integer bmpStatus;

    @Schema(description = "归还日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] returnDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 