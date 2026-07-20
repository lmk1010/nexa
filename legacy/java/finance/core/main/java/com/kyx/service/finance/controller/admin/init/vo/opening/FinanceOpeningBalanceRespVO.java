package com.kyx.service.finance.controller.admin.init.vo.opening;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.kyx.service.business.enums.DictTypeConstants.COMMON_STATUS;

/**
 * 期初余额响应 VO
 */
@Data
@Schema(description = "期初余额")
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class FinanceOpeningBalanceRespVO {

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "账套ID")
    @ExcelProperty("账套ID")
    private Long companyId;

    @Schema(description = "期间")
    @ExcelProperty("期间")
    private String period;

    @Schema(description = "科目编码")
    @ExcelProperty("科目编码")
    private String subjectCode;

    @Schema(description = "科目名称")
    @ExcelProperty("科目名称")
    private String subjectName;

    @Schema(description = "期初余额（正数=增加，负数=减少/冲销）")
    @ExcelProperty("期初余额")
    private BigDecimal openingAmount;

    @Schema(description = "是否锁定")
    @ExcelProperty("是否锁定")
    private Boolean locked;

    @Schema(description = "状态：0-启用，1-停用")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(COMMON_STATUS)
    private Integer status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
