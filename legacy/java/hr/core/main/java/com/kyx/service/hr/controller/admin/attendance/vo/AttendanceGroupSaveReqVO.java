package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 考勤组保存 Request VO")
@Data
public class AttendanceGroupSaveReqVO {

    @Schema(description = "考勤组ID")
    private Long id;

    @Schema(description = "考勤组名称")
    @NotBlank(message = "考勤组名称不能为空")
    private String groupName;

    @Schema(description = "范围类型：ALL/USER/PROFILE")
    private String scopeType;

    @Schema(description = "范围JSON")
    private String scopeJson;

    @Schema(description = "班次规则ID")
    @NotNull(message = "班次规则不能为空")
    private Long shiftRuleId;

    @Schema(description = "状态：0正常 1停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
