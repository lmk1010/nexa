package com.kyx.service.erp.controller.admin.asset.vo.myassets;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 我的归还记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpMyReturnPageReqVO extends PageParam {

    @Schema(description = "资产名称", example = "联想电脑")
    private String assetName;

    @Schema(description = "归还状态：1-已归还，2-已接收确认", example = "1")
    private Integer status;

    @Schema(description = "归还条件：1-完好，2-轻微损坏，3-严重损坏，4-丢失", example = "1")
    private Integer returnCondition;

    @Schema(description = "归还日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] returnDate;

} 