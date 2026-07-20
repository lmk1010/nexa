package com.kyx.service.hr.controller.admin.administrative.meetingroom.vo;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 会议室管理分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrMeetingRoomPageReqVO extends PageParam {

    @Schema(description = "会议室编号")
    private String roomCode;

    @Schema(description = "会议室名称")
    private String roomName;

    @Schema(description = "楼层")
    private String floor;

    @Schema(description = "状态（0启用 1停用）")
    private Integer status;
}
