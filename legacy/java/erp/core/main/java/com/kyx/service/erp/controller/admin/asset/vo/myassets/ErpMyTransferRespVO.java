package com.kyx.service.erp.controller.admin.asset.vo.myassets;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 我转移的资产 Response VO")
@Data
public class ErpMyTransferRespVO {

    @Schema(description = "转移记录编号", example = "1")
    private Long id;

    @Schema(description = "转移编号", example = "TR202312010001")
    private String transferNo;

    @Schema(description = "资产编号", example = "1")
    private Long assetId;

    @Schema(description = "资产编码", example = "ASSET001")
    private String assetNo;

    @Schema(description = "资产名称", example = "联想电脑")
    private String assetName;

    @Schema(description = "资产类型", example = "办公设备")
    private String assetType;

    @Schema(description = "接收人用户编号", example = "2")
    private Long toUserId;

    @Schema(description = "接收人姓名", example = "李四")
    private String toUserName;

    @Schema(description = "接收人部门编号", example = "2")
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

    @Schema(description = "转移状态：0-申请中，1-已完成，2-已拒绝，3-已撤销", example = "1")
    private Integer status;

    @Schema(description = "审批状态：0-待审批，1-审批中，2-审批通过，3-审批拒绝", example = "2")
    private Integer approvalStatus;

    @Schema(description = "审批人用户编号", example = "3")
    private Long approverUserId;

    @Schema(description = "审批人姓名", example = "王五")
    private String approverUserName;

    @Schema(description = "审批时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime approvalTime;

    @Schema(description = "审批备注", example = "审批通过")
    private String approvalRemark;

    @Schema(description = "创建时间")
    private Date createTime;

} 