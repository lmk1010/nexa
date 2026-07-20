package com.kyx.service.hr.controller.admin.administrative.trip.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 出差申请保存 Request VO")
@Data
public class HrTripSaveReqVO {

    @Schema(description = "出差申请ID")
    private Long id;

    @Schema(description = "出差类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "出差类型不能为空")
    private String tripType;

    @Schema(description = "出差城市", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "出差城市不能为空")
    private String destinationCity;

    @Schema(description = "出差地址")
    private String destinationAddress;

    @Schema(description = "出差事由", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "出差事由不能为空")
    private String purpose;

    @Schema(description = "交通方式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "交通方式不能为空")
    private String transportType;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;

    @Schema(description = "出差时长(天)")
    private BigDecimal duration;

    @Schema(description = "预计费用")
    private BigDecimal costEstimate;

    @Schema(description = "应急电话", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "应急电话不能为空")
    private String emergencyPhone;

    @Schema(description = "同行人")
    private String companions;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件")
    private List<String> attachments;

    @Schema(description = "发起人自选审批人 Map")
    private Map<String, List<Long>> startUserSelectAssignees;

    @AssertTrue(message = "结束时间，需要在开始时间之后")
    public boolean isEndTimeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return !endTime.isBefore(startTime);
    }
}
