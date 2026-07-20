package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 打卡记录 Response VO")
@Data
public class AttendanceClockRecordRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "OA user display name")
    private String userNickname;

    @Schema(description = "员工档案ID")
    private Long profileId;

    @Schema(description = "HR profile name")
    private String profileName;

    @Schema(description = "DingTalk user id")
    private String dingUserId;

    @Schema(description = "考勤日期")
    private LocalDate attendanceDate;

    @Schema(description = "打卡类型：IN/OUT")
    private String clockType;

    @Schema(description = "打卡时间")
    private LocalDateTime clockTime;

    @Schema(description = "打卡状态：NORMAL/LATE/EARLY")
    private String clockStatus;

    @Schema(description = "是否由已审批请假覆盖异常")
    private Boolean leaveCovered;

    @Schema(description = "当天已审批请假分钟数")
    private Integer leaveMinutes;

    @Schema(description = "来源类型：MANUAL/DINGTALK")
    private String sourceType;

    @Schema(description = "来源记录ID")
    private String sourceRecordId;

    @Schema(description = "打卡地点名称")
    private String locationName;

    @Schema(description = "打卡地点地址")
    private String locationAddress;

    @Schema(description = "设备信息")
    private String deviceInfo;

    @Schema(description = "备注")
    private String remark;

}
