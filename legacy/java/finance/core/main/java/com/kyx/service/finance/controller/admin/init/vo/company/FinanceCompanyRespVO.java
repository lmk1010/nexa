package com.kyx.service.finance.controller.admin.init.vo.company;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

import static com.kyx.service.business.enums.DictTypeConstants.COMMON_STATUS;
import static com.kyx.service.finance.enums.DictTypeConstants.ACCOUNTING_SYSTEM;
import static com.kyx.service.finance.enums.DictTypeConstants.CURRENCY;

/**
 * 账套响应 VO
 *
 * @author Trae AI
 */
@Data
@Schema(description = "账套信息")
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class FinanceCompanyRespVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("账套编号")
    private Long id;

    @Schema(description = "账套名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("账套名称")
    private String companyName;

    @Schema(description = "账套编码")
    @ExcelProperty("账套编码")
    private String companyCode;

    // 01-企业会计准则	通用企业会计准则	中大型企业
    // 02-小企业会计准则	小企业专用准则	小微企业、个体户
    // 03-民间非营利组织会计制度	非营利机构准则	基金会、协会、民办非企业
    // 04-政府会计制度	行政事业单位准则	机关、事业单位
    // 05-农民专业合作社财务会计制度
    @Schema(description = "会计制度")
    @ExcelProperty(value = "会计制度", converter = DictConvert.class)
    @DictFormat(ACCOUNTING_SYSTEM)
    private String accountingSystem;

    @Schema(description = "货币类型")
    @ExcelProperty(value = "货币类型", converter = DictConvert.class)
    @DictFormat(CURRENCY)
    private String currency;

    @Schema(description = "启用期间")
    @ExcelProperty("启用期间")
    private String startPeriod;

    @Schema(description = "状态：0(启用), 1(停用)")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(COMMON_STATUS)
    private Integer status;

    @Schema(description = "描述")
    @ExcelProperty("描述")
    private String description;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
