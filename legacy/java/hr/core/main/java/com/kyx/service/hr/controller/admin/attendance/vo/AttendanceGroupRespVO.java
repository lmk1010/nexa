package com.kyx.service.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 考勤组 Response VO")
@Data
public class AttendanceGroupRespVO {

    private Long id;

    private String groupName;

    private String scopeType;

    private String scopeJson;

    private Long shiftRuleId;

    private String shiftName;

    private Integer status;

    private String remark;

}
