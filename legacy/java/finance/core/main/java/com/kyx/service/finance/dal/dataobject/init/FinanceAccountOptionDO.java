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

/**
 * 账户扩展选项（企业主体/账户标签/银行支行）
 */
@TableName("finance_account_option")
@KeySequence("finance_account_option_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceAccountOptionDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 选项类型：COMPANY_ENTITY/ACCOUNT_TAG/BANK_BRANCH */
    private String optionType;

    /** 选项值 */
    private String optionValue;

    /** 排序 */
    private Integer sort;

    /** 状态：0 启用，1 停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}
