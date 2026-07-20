package com.kyx.service.erp.controller.admin.asset.vo.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 资产流转记录 Response VO")
@Data
public class ErpAssetLogRespVO {

    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "资产编号", example = "1")
    private Long assetId;

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想电脑")
    private String assetName;

    @Schema(description = "操作类型：1-录入，2-领用申请，3-领用确认，4-归还申请，5-归还确认，6-转移，7-维修，8-报废", example = "1")
    private Integer operationType;

    @Schema(description = "操作类型名称", example = "录入")
    private String operationTypeName;

    @Schema(description = "操作时间", example = "2024-01-01 10:00:00")
    private LocalDateTime operationTime;

    @Schema(description = "操作人编号", example = "1")
    private Long operatorUserId;

    @Schema(description = "操作人姓名", example = "张三")
    private String operatorUserName;

    @Schema(description = "操作人部门编号", example = "1")
    private Long operatorDeptId;

    @Schema(description = "操作人部门名称", example = "技术部")
    private String operatorDeptName;

    @Schema(description = "相关用户编号（领用人/归还人等）", example = "2")
    private Long relatedUserId;

    @Schema(description = "相关用户姓名", example = "李四")
    private String relatedUserName;

    @Schema(description = "相关部门编号", example = "2")
    private Long relatedDeptId;

    @Schema(description = "相关部门名称", example = "销售部")
    private String relatedDeptName;

    @Schema(description = "操作前状态", example = "1")
    private Integer beforeStatus;

    @Schema(description = "操作前状态名称", example = "正常")
    private String beforeStatusName;

    @Schema(description = "操作后状态", example = "2")
    private Integer afterStatus;

    @Schema(description = "操作后状态名称", example = "维修中")
    private String afterStatusName;

    @Schema(description = "业务编号（关联的领用记录ID、归还记录ID等）", example = "1")
    private Long businessId;

    @Schema(description = "操作说明/备注", example = "设备正常领用")
    private String description;

    @Schema(description = "详细信息（JSON格式）", example = "{\"reason\": \"办公需要\"}")
    private String details;

    @Schema(description = "操作结果：1-成功，2-失败", example = "1")
    private Integer result;

    @Schema(description = "IP地址", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "浏览器信息", example = "Chrome/91.0")
    private String userAgent;
} 