package com.kyx.service.erp.controller.admin.asset.vo.assetreturn;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 资产归还记录 Response VO")
@Data
public class ErpAssetReturnRespVO {

    @Schema(description = "归还记录编号", example = "1")
    private Long id;

    @Schema(description = "领用记录编号", example = "1")
    private Long checkoutId;

    @Schema(description = "资产编号", example = "1")
    private Long assetId;

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想电脑")
    private String assetName;

    @Schema(description = "资产类型", example = "办公设备")
    private String assetType;

    @Schema(description = "归还人编号", example = "1")
    private Long returnUserId;

    @Schema(description = "归还人姓名", example = "张三")
    private String returnUserName;

    @Schema(description = "归还部门编号", example = "1")
    private Long returnDeptId;

    @Schema(description = "归还部门名称", example = "技术部")
    private String returnDeptName;

    @Schema(description = "归还日期")
    private LocalDate returnDate;

    @Schema(description = "归还状态：1-完好，2-轻微损坏，3-严重损坏，4-丢失", example = "1")
    private Integer returnCondition;

    @Schema(description = "归还备注", example = "设备完好")
    private String returnRemark;

    @Schema(description = "接收人编号", example = "1")
    private Long receiverUserId;

    @Schema(description = "接收人姓名", example = "李四")
    private String receiverUserName;

    @Schema(description = "接收时间")
    private LocalDateTime receiverTime;

    @Schema(description = "接收备注", example = "已确认接收")
    private String receiverRemark;

    @Schema(description = "归还状态：1-已归还，2-已接收确认", example = "1")
    private Integer status;

    @Schema(description = "BPM流程实例ID", example = "proc-inst-123")
    private String processInstanceId;

    @Schema(description = "BMP流程状态", example = "1")
    private Integer bmpStatus;

    @Schema(description = "备注", example = "测试备注")
    private String remark;

    @Schema(description = "领用日期")
    private LocalDate checkoutDate;

    @Schema(description = "预计归还日期")
    private LocalDate expectedReturnDate;

    @Schema(description = "领用原因", example = "办公使用")
    private String checkoutReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

} 