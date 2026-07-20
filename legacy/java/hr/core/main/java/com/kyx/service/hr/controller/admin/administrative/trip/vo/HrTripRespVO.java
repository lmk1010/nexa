package com.kyx.service.hr.controller.admin.administrative.trip.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 出差申请 Response VO")
@Data
public class HrTripRespVO {

    @Schema(description = "出差申请ID")
    private Long id;

    @Schema(description = "流程状态")
    private Integer status;

    @Schema(description = "出差类型")
    private String tripType;

    @Schema(description = "出差城市")
    private String destinationCity;

    @Schema(description = "出差地址")
    private String destinationAddress;

    @Schema(description = "出差事由")
    private String purpose;

    @Schema(description = "交通方式")
    private String transportType;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "出差时长(天)")
    private BigDecimal duration;

    @Schema(description = "预计费用")
    private BigDecimal costEstimate;

    @Schema(description = "应急电话")
    private String emergencyPhone;

    @Schema(description = "同行人")
    private String companions;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件")
    private List<String> attachments;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
