package com.kyx.service.hr.controller.admin.administrative.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 请假申请 Response VO")
@Data
public class HrLeaveRespVO {

    @Schema(description = "请假申请ID")
    private Long id;

    @Schema(description = "流程状态")
    private Integer status;

    @Schema(description = "申请人用户ID")
    private Long userId;

    @Schema(description = "申请人姓名")
    private String userName;

    @Schema(description = "申请人手机号")
    private String userMobile;

    @Schema(description = "请假/调休")
    private String leaveCategory;

    @Schema(description = "请假类型")
    private String leaveType;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "时长")
    private BigDecimal duration;

    @Schema(description = "应急电话")
    private String emergencyPhone;

    @Schema(description = "工作交接")
    private String workHandover;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件")
    private List<String> attachments;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
