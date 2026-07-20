package com.kyx.service.erp.controller.admin.asset.vo.borrow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 资产借用记录 Response VO
 */
@Schema(description = "管理后台 - ERP 资产借用记录 Response VO")
@Data
public class ErpAssetBorrowRespVO {

    @Schema(description = "借用记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long id;

    @Schema(description = "借用编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOR2023001")
    private String borrowNo;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long assetId;

    @Schema(description = "资产编码", example = "AST001")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想笔记本电脑")
    private String assetName;

    @Schema(description = "借用人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long borrowUserId;

    @Schema(description = "借用人姓名", example = "张三")
    private String borrowUserName;

    @Schema(description = "借用部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long borrowDeptId;

    @Schema(description = "借用部门名称", example = "技术部")
    private String borrowDeptName;

    @Schema(description = "借用日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate borrowDate;

    @Schema(description = "预计归还日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate expectedReturnDate;

    @Schema(description = "实际归还日期")
    private LocalDate actualReturnDate;

    @Schema(description = "借用原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "项目需要")
    private String borrowReason;

    @Schema(description = "借用用途", example = "用于XX项目开发")
    private String borrowPurpose;

    @Schema(description = "借用状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "归还状态", example = "1")
    private Integer returnCondition;

    @Schema(description = "归还备注", example = "完好归还")
    private String returnRemark;

    @Schema(description = "审批人编号", example = "123")
    private Long approverUserId;

    @Schema(description = "审批人姓名", example = "李四")
    private String approverUserName;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "审批状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer approvalStatus;

    @Schema(description = "审批备注", example = "同意借用")
    private String approvalRemark;

    @Schema(description = "BMP状态", example = "1")
    private Integer bmpStatus;

    @Schema(description = "BMP流程实例编号", example = "PROC_INST_001")
    private String bmpProcessInstanceId;

    @Schema(description = "备注", example = "特殊说明")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;
} 