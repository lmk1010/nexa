package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Schema(description = "管理后台 - ERP 盘点计划新增/修改 Request VO")
@Data
public class ErpInventoryPlanSaveReqVO {

    @Schema(description = "盘点计划编号", example = "1")
    private Long id;

    @Schema(description = "计划名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024年第一季度全面盘点")
    @NotEmpty(message = "计划名称不能为空")
    private String planName;

    @Schema(description = "盘点周期", requiredMode = Schema.RequiredMode.REQUIRED, example = "quarterly")
    @NotEmpty(message = "盘点周期不能为空")
    private String planType;

    @Schema(description = "盘点方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "full")
    @NotEmpty(message = "盘点方式不能为空")
    private String method;

    @Schema(description = "计划开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @Schema(description = "计划结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    @Schema(description = "计划描述", example = "对所有资产进行季度盘点，确保资产数据准确性")
    private String description;

    @Schema(description = "抽样比例（1-100）", example = "20")
    private Integer sampleRate;

    @Schema(description = "抽样方式", example = "random")
    private String sampleMethod;

    @Schema(description = "选择的部门ID列表", example = "[1, 2, 3]")
    private List<Long> departmentIds;

    @Schema(description = "选择的使用人ID列表", example = "[1, 2, 3]")
    private List<Long> userIds;

    @Schema(description = "选择的资产位置ID列表", example = "[1, 2, 3]")
    private List<Long> locationIds;

    @Schema(description = "盘点负责人用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "盘点负责人不能为空")
    private Long responsiblePersonId;

    @Schema(description = "扫码员用户ID列表", example = "[1, 2, 3]")
    private List<Long> scannerIds;

    @Schema(description = "复核人员用户ID列表", example = "[1, 2, 3]")
    private List<Long> reviewerIds;

    @Schema(description = "是否锁定待盘库存", example = "true")
    private Boolean lockInventory;

    @Schema(description = "是否自动导出盘点清单", example = "true")
    private Boolean autoExportList;

    @Schema(description = "备注", example = "季度盘点注意事项")
    private String remark;

} 