package com.kyx.service.hr.controller.admin.administrative.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 会议室预约保存 Request VO")
@Data
public class HrMeetingSaveReqVO {

    @Schema(description = "会议预约ID")
    private Long id;

    @Schema(description = "会议室编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "会议室编号不能为空")
    private String roomId;

    @Schema(description = "会议室名称")
    private String roomName;

    @Schema(description = "会议主题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "会议主题不能为空")
    private String meetingTitle;

    @Schema(description = "会议类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "会议类型不能为空")
    private String meetingType;

    @Schema(description = "会议组织人")
    private String organizer;

    @Schema(description = "参会人数")
    @NotNull(message = "参会人数不能为空")
    @Min(value = 1, message = "参会人数必须大于0")
    private Integer attendees;

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

    @Schema(description = "会议时长(小时)")
    private BigDecimal duration;

    @Schema(description = "设备需求")
    private List<String> equipment;

    @Schema(description = "备注")
    private String remark;

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
