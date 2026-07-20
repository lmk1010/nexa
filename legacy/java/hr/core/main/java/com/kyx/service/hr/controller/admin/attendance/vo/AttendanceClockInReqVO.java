package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 打卡 Request VO")
@Data
public class AttendanceClockInReqVO {

    @Schema(description = "打卡类型：IN/OUT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "打卡类型不能为空")
    private String clockType;

    @Schema(description = "打卡地点名称")
    @Size(max = 128, message = "地点名称长度不能超过128")
    private String locationName;

    @Schema(description = "打卡地点地址")
    @Size(max = 255, message = "地点地址长度不能超过255")
    private String locationAddress;

    @Schema(description = "设备信息")
    @Size(max = 128, message = "设备信息长度不能超过128")
    private String deviceInfo;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

}
