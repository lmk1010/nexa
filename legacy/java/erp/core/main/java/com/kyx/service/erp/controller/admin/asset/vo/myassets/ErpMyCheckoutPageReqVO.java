package com.kyx.service.erp.controller.admin.asset.vo.myassets;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 我的申请领用资产分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpMyCheckoutPageReqVO extends PageParam {

    @Schema(description = "资产名称", example = "联想电脑")
    private String assetName;

    @Schema(description = "领用状态", example = "1")
    private Integer status;

    @Schema(description = "审批状态", example = "1")
    private Integer approvalStatus;

    @Schema(description = "领用时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] checkoutDate;

} 