package com.kyx.service.erp.controller.admin.asset.vo.transfer;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static com.kyx.foundation.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 资产转移分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAssetTransferPageReqVO extends PageParam {

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

    @Schema(description = "接收人用户ID", example = "2")
    private Long toUserId;

    @Schema(description = "转移人部门ID", example = "1")
    private Long fromDeptId;

    @Schema(description = "接收人部门ID", example = "2")
    private Long toDeptId;

    @Schema(description = "转移状态：0-申请中，1-已完成，2-已拒绝，3-已撤销", example = "0")
    private Integer status;

    @Schema(description = "审批状态：0-待审批，1-审批中，2-审批通过，3-审批拒绝", example = "0")
    private Integer approvalStatus;

    @Schema(description = "转移日期-开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime transferDateStart;

    @Schema(description = "转移日期-结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime transferDateEnd;

    @Schema(description = "创建时间-开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeStart;

    @Schema(description = "创建时间-结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTimeEnd;

} 