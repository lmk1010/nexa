package com.kyx.service.erp.controller.admin.asset.vo.redistribution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - ERP 资产调拨保存 Request VO")
@Data
public class ErpAssetRedistributionSaveReqVO {

    @Schema(description = "调拨记录ID", example = "1")
    private Long id;

    @Schema(description = "资产ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "资产ID列表不能为空")
    private List<Long> assetIds;

    @Schema(description = "调拨前部门ID", example = "1")
    private Long fromDeptId;

    @Schema(description = "调拨到部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "调拨到部门ID不能为空")
    private Long toDeptId;

    @Schema(description = "调拨前位置", example = "办公楼A栋1楼")
    private String fromLocation;

    @Schema(description = "调拨到位置", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公楼B栋2楼")
    @NotEmpty(message = "调拨到位置不能为空")
    private String toLocation;

    @Schema(description = "调拨日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "调拨日期不能为空")
    private LocalDateTime allocationDate;

    @Schema(description = "调拨原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "部门重组")
    @NotEmpty(message = "调拨原因不能为空")
    private String allocationReason;

    @Schema(description = "调拨备注", example = "批量调拨办公设备")
    private String remark;

    @Schema(description = "BPM 用户选择的审批人 Map", example = "{\"user1\": [2, 3]}")
    private Map<String, List<Long>> startUserSelectAssignees;
} 