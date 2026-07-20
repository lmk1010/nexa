package com.kyx.service.erp.controller.admin.asset.vo.myassets;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 我转移的资产分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpMyTransferPageReqVO extends PageParam {

    @Schema(description = "资产名称", example = "联想电脑")
    private String assetName;

    @Schema(description = "接收人姓名", example = "李四")
    private String toUserName;

    @Schema(description = "转移状态", example = "1")
    private Integer status;

    @Schema(description = "审批状态", example = "2")
    private Integer approvalStatus;

    @Schema(description = "转移日期-开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] transferDate;

} 