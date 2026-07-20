package com.kyx.service.finance.controller.admin.init.vo.account;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.kyx.foundation.excel.core.annotations.DictFormat;
import com.kyx.foundation.excel.core.convert.DictConvert;
import com.kyx.service.business.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.kyx.service.finance.enums.DictTypeConstants.ACCOUNT_TYPE;

/**
 * 账户响应 VO
 *
 * @author xyang
 */
@Data
@Schema(description = "账户")
@ExcelIgnoreUnannotated
@Accessors(chain = true)
public class FinanceAccountRespVO {

    @Schema(description = "账户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("账户编号")
    private Long id;

    @Schema(description = "账户别名")
    @ExcelProperty("账户别名")
    private String accountAlias;

    @Schema(description = "账户类型")
    @ExcelProperty(value = "账户类型", converter = DictConvert.class)
    @DictFormat(ACCOUNT_TYPE)
    private String accountType;

    @Schema(description = "账号")
    @ExcelProperty("账号")
    private String accountNumber;

    @Schema(description = "税号")
    @ExcelProperty("税号")
    private String taxNo;

    @Schema(description = "余额")
    @ExcelProperty("余额")
    private BigDecimal balance;

    @Schema(description = "币种")
    @ExcelProperty("币种")
    private String currency;

    @Schema(description = "状态：0 启用，1 停用")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "银行名称")
    @ExcelProperty("银行名称")
    private String bankName;

    @Schema(description = "省编码")
    @ExcelProperty("省编码")
    private String provinceCode;

    @Schema(description = "市编码")
    @ExcelProperty("市编码")
    private String cityCode;

    @Schema(description = "区编码")
    @ExcelProperty("区编码")
    private String districtCode;

    @Schema(description = "支行名称")
    @ExcelProperty("支行名称")
    private String branchName;

    @Schema(description = "企业主体")
    @ExcelProperty("企业主体")
    private String companyEntity;

    @Schema(description = "账户标签")
    @ExcelProperty("账户标签")
    private String accountTagText;

    @Schema(description = "收款是否需要手续费")
    @ExcelProperty("收款手续费")
    private Boolean receiptFeeEnabled;

    @Schema(description = "付款是否需要手续费")
    @ExcelProperty("付款手续费")
    private Boolean paymentFeeEnabled;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建者")
    @ExcelProperty("创建者")
    private String creator;
}
