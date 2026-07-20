package com.kyx.service.erp.controller.admin.asset.vo.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

@Schema(description = "管理后台 - ERP 盘点计划 Response VO")
@Data
public class ErpInventoryPlanRespVO {

    @Schema(description = "盘点计划编号", example = "1")
    private Long id;

    @Schema(description = "计划编号", example = "INV-2024-001")
    private String planNo;

    @Schema(description = "计划名称", example = "2024年第一季度全面盘点")
    private String planName;

    @Schema(description = "盘点周期", example = "quarterly")
    private String planType;

    @Schema(description = "盘点方式", example = "full")
    private String method;

    @Schema(description = "计划开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @Schema(description = "计划结束时间")
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

    @Schema(description = "盘点负责人用户ID", example = "1")
    private Long responsiblePersonId;

    @Schema(description = "盘点负责人姓名", example = "张三")
    private String responsiblePersonName;

    @Schema(description = "扫码员用户ID列表", example = "[1, 2, 3]")
    private List<Long> scannerIds;

    @Schema(description = "扫码员姓名列表", example = "[\"李四\", \"王五\"]")
    private List<String> scannerNames;

    @Schema(description = "复核人员用户ID列表", example = "[1, 2, 3]")
    private List<Long> reviewerIds;

    @Schema(description = "复核人员姓名列表", example = "[\"赵六\", \"钱七\"]")
    private List<String> reviewerNames;

    @Schema(description = "是否锁定待盘库存", example = "true")
    private Boolean lockInventory;

    @Schema(description = "是否自动导出盘点清单", example = "true")
    private Boolean autoExportList;

    @Schema(description = "盘点计划状态", example = "1")
    private Integer status;

    @Schema(description = "总资产数量", example = "1200")
    private Integer totalAssetCount;

    @Schema(description = "已完成数量", example = "850")
    private Integer completedAssetCount;

    @Schema(description = "实际开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date actualStartTime;

    @Schema(description = "实际结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date actualEndTime;

    @Schema(description = "审批人用户ID", example = "1")
    private Long approvalUserId;

    @Schema(description = "审批人姓名", example = "管理员")
    private String approvalUserName;

    @Schema(description = "审批时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date approvalTime;

    @Schema(description = "审批备注", example = "审批通过")
    private String approvalRemark;

    @Schema(description = "BPM流程实例ID", example = "123456")
    private String processInstanceId;

    @Schema(description = "备注", example = "季度盘点注意事项")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "更新者")
    private String updater;

}
