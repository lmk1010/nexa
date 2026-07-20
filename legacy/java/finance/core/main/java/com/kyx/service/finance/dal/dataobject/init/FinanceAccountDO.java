package com.kyx.service.finance.dal.dataobject.init;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 资金账户表 DO
 *
 * @author xyang
 */
@TableName("finance_account")
@KeySequence("finance_account_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceAccountDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 账户别名 */
    private String accountAlias;

    /** 账户类型 */
    private String accountType;

    /** 账号 */
    private String accountNumber;

    /** 税号 */
    private String taxNo;

    /** 余额 */
    private BigDecimal balance;

    /** 币种 */
    private String currency;

    /** 状态：0 启用，1 停用 */
    private Integer status;

    /** 银行名称 */
    private String bankName;

    /** 省编码 */
    private String provinceCode;

    /** 市编码 */
    private String cityCode;

    /** 区编码 */
    private String districtCode;

    /** 支行名称 */
    private String branchName;

    /** 企业主体 */
    private String companyEntity;

    /** 账户标签文本（逗号分隔） */
    private String accountTagText;

    /** 收款是否需要手续费 */
    private Boolean receiptFeeEnabled;

    /** 付款是否需要手续费 */
    private Boolean paymentFeeEnabled;

    /** 备注 */
    private String remark;
}
