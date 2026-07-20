package com.kyx.service.erp.controller.admin.asset.vo.myassets;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 我的申请领用资产 Response VO")
@Data
public class ErpMyCheckoutRespVO {

    @Schema(description = "领用记录编号", example = "1")
    private Long id;

    @Schema(description = "资产编号", example = "1")
    private Long assetId;

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想电脑")
    private String assetName;

    @Schema(description = "资产类型", example = "办公设备")
    private String assetType;

    @Schema(description = "领用日期")
    private LocalDate checkoutDate;

    @Schema(description = "预计归还日期")
    private LocalDate expectedReturnDate;

    @Schema(description = "实际归还日期")
    private LocalDate actualReturnDate;

    @Schema(description = "领用原因", example = "办公使用")
    private String checkoutReason;

    @Schema(description = "领用状态：1-领用中，2-已归还，3-逾期未还，4-资产损坏，5-资产丢失", example = "1")
    private Integer status;

    @Schema(description = "归还状态：1-完好，2-轻微损坏，3-严重损坏，4-丢失", example = "1")
    private Integer returnCondition;

    @Schema(description = "归还备注", example = "设备完好")
    private String returnRemark;

    @Schema(description = "审批人编号", example = "1")
    private Long approverUserId;

    @Schema(description = "审批人姓名", example = "李四")
    private String approverUserName;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "审批状态：1-待审批，2-审批通过，3-审批拒绝", example = "1")
    private Integer approvalStatus;

    @Schema(description = "审批备注", example = "同意申请")
    private String approvalRemark;

    @Schema(description = "BMP流程状态：1-流程中，2-已完成，3-已拒绝，4-已撤销", example = "1")
    private Integer bmpStatus;

    @Schema(description = "备注", example = "测试备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

} 