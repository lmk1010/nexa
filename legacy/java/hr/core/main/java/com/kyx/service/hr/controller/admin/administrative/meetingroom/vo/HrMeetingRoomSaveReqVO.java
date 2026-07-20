package com.kyx.service.hr.controller.admin.administrative.meetingroom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 会议室管理保存 Request VO")
@Data
public class HrMeetingRoomSaveReqVO {

    @Schema(description = "会议室ID")
    private Long id;

    @Schema(description = "会议室编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "会议室编号不能为空")
    private String roomCode;

    @Schema(description = "会议室名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "会议室名称不能为空")
    private String roomName;

    @Schema(description = "楼层")
    private String floor;

    @Schema(description = "位置")
    private String location;

    @Schema(description = "容纳人数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "容纳人数不能为空")
    @Min(value = 1, message = "容纳人数必须大于0")
    private Integer capacity;

    @Schema(description = "设备")
    private List<String> equipment;

    @Schema(description = "状态（0启用 1停用）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
