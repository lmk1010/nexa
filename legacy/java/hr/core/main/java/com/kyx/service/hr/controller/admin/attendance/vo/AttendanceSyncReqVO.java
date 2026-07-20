package com.kyx.service.hr.controller.admin.attendance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 钉钉考勤同步 Request VO")
@Data
public class AttendanceSyncReqVO {

    @Schema(description = "来源系统")
    private String sourceType;

    @Schema(description = "同步记录", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "同步记录不能为空")
    @Valid
    private List<Record> records;

    @Schema(description = "同步记录")
    @Data
    public static class Record {

        @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        @Schema(description = "员工档案ID")
        private Long profileId;

        @Schema(description = "考勤日期", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "考勤日期不能为空")
        @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY)
        private LocalDate attendanceDate;

        @Schema(description = "打卡类型：IN/OUT", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "打卡类型不能为空")
        private String clockType;

        @Schema(description = "打卡时间", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "打卡时间不能为空")
        @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime clockTime;

        @Schema(description = "来源记录ID（钉钉流水）", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "来源记录ID不能为空")
        private String sourceRecordId;

        @Schema(description = "打卡状态：NORMAL/LATE/EARLY")
        private String clockStatus;

        @Schema(description = "打卡地点名称")
        private String locationName;

        @Schema(description = "打卡地点地址")
        private String locationAddress;

        @Schema(description = "设备信息")
        private String deviceInfo;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "原始payload(JSON字符串)")
        private String rawPayload;

    }

}
