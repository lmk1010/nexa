package com.kyx.service.erp.controller.admin.asset.vo.checkout;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 资产领用记录 Response VO")
@Data
public class ErpAssetCheckoutRespVO {

    @Schema(description = "领用记录编号", example = "1")
    private Long id;

    @Schema(description = "资产编号", example = "1")
    private Long assetId;

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想电脑")
    private String assetName;

    @Schema(description = "领用人编号", example = "1")
    private Long checkoutUserId;

    @Schema(description = "领用人姓名", example = "张三")
    private String checkoutUserName;

    @Schema(description = "领用部门编号", example = "1")
    private Long checkoutDeptId;

    @Schema(description = "领用部门名称", example = "技术部")
    private String checkoutDeptName;

    @Schema(description = "领用日期")
    private LocalDate checkoutDate;

    @Schema(description = "预计归还日期")
    private LocalDate expectedReturnDate;

    @Schema(description = "实际归还日期")
    private LocalDate actualReturnDate;

    @Schema(description = "领用原因", example = "办公使用")
    private String checkoutReason;

    @Schema(description = "领用状态", example = "1")
    private Integer status;

    @Schema(description = "归还状态", example = "1")
    private Integer returnCondition;

    @Schema(description = "归还备注", example = "设备完好")
    private String returnRemark;

    @Schema(description = "审批人编号", example = "1")
    private Long approverUserId;

    @Schema(description = "审批人姓名", example = "李四")
    private String approverUserName;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "审批状态", example = "1")
    private Integer approvalStatus;

    @Schema(description = "审批备注", example = "同意")
    private String approvalRemark;

    @Schema(description = "BPM流程实例ID", example = "proc-inst-123")
    private String processInstanceId;

    @Schema(description = "BMP流程状态", example = "1")
    private Integer bmpStatus;

    @Schema(description = "备注", example = "测试备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

} 