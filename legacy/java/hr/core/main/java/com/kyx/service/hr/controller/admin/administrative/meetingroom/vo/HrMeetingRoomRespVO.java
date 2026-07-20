package com.kyx.service.hr.controller.admin.administrative.meetingroom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 会议室管理 Response VO")
@Data
public class HrMeetingRoomRespVO {

    @Schema(description = "会议室ID")
    private Long id;

    @Schema(description = "会议室编号")
    private String roomCode;

    @Schema(description = "会议室名称")
    private String roomName;

    @Schema(description = "楼层")
    private String floor;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "容纳人数")
    private Integer capacity;

    @Schema(description = "设备")
    private List<String> equipment;

    @Schema(description = "状态（0启用 1停用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    private String updater;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
