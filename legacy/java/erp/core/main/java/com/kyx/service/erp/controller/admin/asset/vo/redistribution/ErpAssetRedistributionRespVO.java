package com.kyx.service.erp.controller.admin.asset.vo.redistribution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 资产调拨 Response VO")
@Data
public class ErpAssetRedistributionRespVO {

    @Schema(description = "调拨记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "调拨编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "RD202312010001")
    private String redistributionNo;

    @Schema(description = "调拨前部门ID", example = "1")
    private Long fromDeptId;

    @Schema(description = "调拨前部门名称", example = "技术部")
    private String fromDeptName;

    @Schema(description = "调拨到部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Long toDeptId;

    @Schema(description = "调拨到部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "市场部")
    private String toDeptName;

    @Schema(description = "调拨前位置", example = "办公楼A栋1楼")
    private String fromLocation;

    @Schema(description = "调拨到位置", requiredMode = Schema.RequiredMode.REQUIRED, example = "办公楼B栋2楼")
    private String toLocation;

    @Schema(description = "调拨日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime allocationDate;

    @Schema(description = "调拨原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "部门重组")
    private String allocationReason;

    @Schema(description = "调拨备注", example = "批量调拨办公设备")
    private String remark;

    @Schema(description = "调拨状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "审批人用户ID", example = "1")
    private Long approverUserId;

    @Schema(description = "审批人用户名", example = "管理员")
    private String approverUserName;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "审批状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer approvalStatus;

    @Schema(description = "审批备注", example = "审批通过")
    private String approvalRemark;

    @Schema(description = "BMP流程状态", example = "1")
    private Integer bmpStatus;

    @Schema(description = "BMP流程实例ID", example = "process_123")
    private String processInstanceId;

    @Schema(description = "确认接收时间")
    private LocalDateTime confirmTime;

    @Schema(description = "确认接收备注", example = "已确认接收")
    private String confirmRemark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "资产项列表")
    private List<Item> items;

    @Schema(description = "资产调拨项")
    @Data
    public static class Item {

        @Schema(description = "调拨项ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long id;

        @Schema(description = "资产ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Long assetId;

        @Schema(description = "资产编码", example = "ASSET001")
        private String assetNo;

        @Schema(description = "资产名称", example = "联想电脑")
        private String assetName;

        @Schema(description = "调拨前的资产状态", example = "1")
        private Integer originalStatus;

        @Schema(description = "调拨前的资产位置", example = "办公楼A栋1楼")
        private String originalLocation;

        @Schema(description = "调拨前的所属部门ID", example = "1")
        private Long originalDeptId;

        @Schema(description = "调拨前的所属部门名称", example = "技术部")
        private String originalDeptName;

        @Schema(description = "调拨前的使用人ID", example = "1")
        private Long originalUserId;

        @Schema(description = "调拨前的使用人名称", example = "张三")
        private String originalUserName;

        @Schema(description = "备注", example = "特殊处理")
        private String remark;
    }
} 