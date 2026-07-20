package com.kyx.service.erp.controller.admin.asset.vo.borrow;

import com.kyx.foundation.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

/**
 * ERP 资产借用记录分页 Request VO
 */
@Schema(description = "管理后台 - ERP 资产借用记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAssetBorrowPageReqVO extends PageParam {

    @Schema(description = "借用编号", example = "BOR2023001")
    private String borrowNo;

    @Schema(description = "资产编号", example = "123")
    private Long assetId;

    @Schema(description = "借用人编号", example = "123")
    private Long borrowUserId;

    @Schema(description = "借用部门编号", example = "123")
    private Long borrowDeptId;

    @Schema(description = "借用日期")
    private LocalDate[] borrowDate;

    @Schema(description = "预计归还日期")
    private LocalDate[] expectedReturnDate;

    @Schema(description = "借用状态", example = "1")
    private Integer status;

    @Schema(description = "审批状态", example = "1")
    private Integer approvalStatus;
} 