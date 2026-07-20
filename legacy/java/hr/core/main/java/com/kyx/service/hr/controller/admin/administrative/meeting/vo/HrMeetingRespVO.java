package com.kyx.service.hr.controller.admin.administrative.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 会议室预约 Response VO")
@Data
public class HrMeetingRespVO {

    @Schema(description = "会议预约ID")
    private Long id;

    @Schema(description = "流程状态")
    private Integer status;

    @Schema(description = "会议室编号")
    private String roomId;

    @Schema(description = "会议室名称")
    private String roomName;

    @Schema(description = "会议主题")
    private String meetingTitle;

    @Schema(description = "会议类型")
    private String meetingType;

    @Schema(description = "会议组织人")
    private String organizer;

    @Schema(description = "参会人数")
    private Integer attendees;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "会议时长(小时)")
    private BigDecimal duration;

    @Schema(description = "设备需求")
    private List<String> equipment;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
