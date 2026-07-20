package com.kyx.service.hr.controller.admin.administrative.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 假期类型保存 Request VO")
@Data
public class HrLeaveTypeSaveReqVO {

    @Schema(description = "类型ID")
    private Long id;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "类型编码")
    private String typeCode;

    @Schema(description = "是否启用余额")
    private Boolean balanceEnabled;

    @Schema(description = "最小单位：HOUR/DAY")
    private String minUnit;

    @Schema(description = "是否带薪")
    private Boolean paid;

    @Schema(description = "是否必传附件")
    private Boolean attachmentRequired;

    @Schema(description = "年度默认额度")
    private BigDecimal annualDefaultAmount;

    @Schema(description = "状态：0正常 1停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

}
