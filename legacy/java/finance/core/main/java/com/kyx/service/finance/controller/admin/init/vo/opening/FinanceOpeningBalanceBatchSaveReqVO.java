package com.kyx.service.finance.controller.admin.init.vo.opening;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

/**
 * 期初余额批量保存请求 VO
 */
@Data
@Schema(description = "期初余额批量保存请求")
public class FinanceOpeningBalanceBatchSaveReqVO {

    @Schema(description = "账套ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套ID不能为空")
    private Long companyId;

    @Schema(description = "期间，格式yyyyMM", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "期间不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "期间格式必须为yyyyMM")
    private String period;

    @Schema(description = "期初余额明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "期初余额明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    @Schema(description = "期初余额明细")
    public static class Item {

        @Schema(description = "科目编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "科目编码不能为空")
        private String subjectCode;

        @Schema(description = "科目名称")
        private String subjectName;

        @Schema(description = "期初余额（正数=增加，负数=减少/冲销）")
        private BigDecimal openingAmount;

        @Schema(description = "状态：0-启用，1-停用")
        private Integer status;

        @Schema(description = "备注")
        private String remark;
    }
}
