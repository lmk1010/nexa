package com.kyx.service.erp.controller.admin.asset.vo.transfer;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 资产转移 Response VO")
@Data
public class ErpAssetTransferRespVO {

    @Schema(description = "转移记录ID", example = "1")
    private Long id;

    @Schema(description = "转移编号", example = "TR202312010001")
    private String transferNo;

    @Schema(description = "资产ID", example = "1")
    private Long assetId;

    @Schema(description = "资产编号", example = "A202312010001")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想ThinkPad笔记本")
    private String assetName;

    @Schema(description = "转移人用户ID", example = "1")
    private Long fromUserId;

    @Schema(description = "转移人姓名", example = "张三")
    private String fromUserName;

    @Schema(description = "转移人部门ID", example = "1")
    private Long fromDeptId;

    @Schema(description = "转移人部门名称", example = "技术部")
    private String fromDeptName;

    @Schema(description = "接收人用户ID", example = "2")
    private Long toUserId;

    @Schema(description = "接收人姓名", example = "李四")
    private String toUserName;

    @Schema(description = "接收人部门ID", example = "2")
    private Long toDeptId;

    @Schema(description = "接收人部门名称", example = "运营部")
    private String toDeptName;

    @Schema(description = "转移日期")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime transferDate;

    @Schema(description = "转移原因", example = "人员调动")
    private String transferReason;

    @Schema(description = "转移备注", example = "请注意保护设备")
    private String transferRemark;

    @Schema(description = "转移状态：0-申请中，1-已完成，2-已拒绝，3-已撤销", example = "0")
    private Integer status;

    @Schema(description = "审批人用户ID", example = "3")
    private Long approverUserId;

    @Schema(description = "审批人姓名", example = "王五")
    private String approverUserName;

    @Schema(description = "审批时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime approvalTime;

    @Schema(description = "审批状态：0-待审批，1-审批中，2-审批通过，3-审批拒绝", example = "0")
    private Integer approvalStatus;

    @Schema(description = "审批备注", example = "同意转移")
    private String approvalRemark;

    @Schema(description = "BPM流程状态：1-流程中，2-已完成，3-已拒绝，4-已撤销", example = "1")
    private Integer bmpStatus;

    @Schema(description = "BPM流程实例ID", example = "123456")
    private String processInstanceId;

    @Schema(description = "接收确认时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime confirmTime;

    @Schema(description = "接收确认备注", example = "已收到设备")
    private String confirmRemark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTime;

} 