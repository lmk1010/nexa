package com.kyx.service.erp.api.asset.vo.borrow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * ERP 资产借用记录保存 Request VO
 */
@Schema(description = "管理后台 - ERP 资产借用记录保存 Request VO")
@Data
public class ErpAssetBorrowSaveReqVO {

    @Schema(description = "借用记录编号", example = "123")
    private Long id;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "借用人编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    @NotNull(message = "借用人编号不能为空")
    private Long borrowUserId;

    @Schema(description = "借用部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    @NotNull(message = "借用部门编号不能为空")
    private Long borrowDeptId;

    @Schema(description = "借用日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "借用日期不能为空")
    private LocalDate borrowDate;

    @Schema(description = "预计归还日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "预计归还日期不能为空")
    private LocalDate expectedReturnDate;

    @Schema(description = "借用原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "项目需要")
    @NotNull(message = "借用原因不能为空")
    private String borrowReason;

    @Schema(description = "借用用途", example = "用于XX项目开发")
    private String borrowPurpose;

    @Schema(description = "备注", example = "特殊说明")
    private String remark;
} 