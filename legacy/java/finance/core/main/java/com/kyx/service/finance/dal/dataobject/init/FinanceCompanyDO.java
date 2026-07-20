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
 * 账套表 DO
 *
 * @author xyang
 */
@TableName("finance_company")
@KeySequence("finance_company_seq") 
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceCompanyDO extends TenantBaseDO {

    /**
     * 账套ID
     */
    @TableId
    private Long id;

    /**
     * 账套名称
     */
    private String companyName;

    /**
     * 账套编码
     */
    private String companyCode;

    /**
     * 会计制度
     */
    private String accountingSystem;

    /**
     * 本位币
     */
    private String currency;

    /**
     * 启用期间
     */
    private String startPeriod;

    /**
     * 当前结账期间
     */
    private String currentClosePeriod;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 描述
     */
    private String description;

}
