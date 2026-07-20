package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 我的今日打卡 Response VO")
@Data
public class AttendanceMyTodayRespVO {

    @Schema(description = "考勤日期")
    private LocalDate attendanceDate;

    @Schema(description = "是否已上班打卡")
    private Boolean hasClockIn;

    @Schema(description = "是否已下班打卡")
    private Boolean hasClockOut;

    @Schema(description = "首次上班打卡时间")
    private LocalDateTime firstClockInTime;

    @Schema(description = "末次下班打卡时间")
    private LocalDateTime lastClockOutTime;

    @Schema(description = "今日打卡明细")
    private List<RecordItem> records;

    @Schema(description = "打卡明细")
    @Data
    public static class RecordItem {

        @Schema(description = "主键")
        private Long id;

        @Schema(description = "打卡类型：IN/OUT")
        private String clockType;

        @Schema(description = "打卡时间")
        private LocalDateTime clockTime;

        @Schema(description = "打卡状态：NORMAL/LATE/EARLY")
        private String clockStatus;

        @Schema(description = "来源类型：MANUAL/DINGTALK")
        private String sourceType;

        @Schema(description = "打卡地点名称")
        private String locationName;

        @Schema(description = "备注")
        private String remark;

    }

}
