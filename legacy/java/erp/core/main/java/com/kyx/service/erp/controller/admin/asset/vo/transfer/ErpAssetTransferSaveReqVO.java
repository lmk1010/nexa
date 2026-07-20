package com.kyx.service.erp.controller.admin.asset.vo.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 资产转移新增/修改 Request VO")
@Data
public class ErpAssetTransferSaveReqVO {

    @Schema(description = "转移记录ID", example = "1")
    private Long id;

    @Schema(description = "资产ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    @Schema(description = "接收人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "接收人用户ID不能为空")
    private Long toUserId;

    @Schema(description = "转移日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "转移日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime transferDate;

    @Schema(description = "转移原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "人员调动")
    @NotBlank(message = "转移原因不能为空")
    private String transferReason;

    @Schema(description = "转移备注", example = "请注意保护设备")
    private String transferRemark;

    @Schema(description = "BPM - 发起人自选审批人")
    private Map<String, List<Long>> startUserSelectAssignees;

} 