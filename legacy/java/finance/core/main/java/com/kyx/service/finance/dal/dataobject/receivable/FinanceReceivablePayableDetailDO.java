package com.kyx.service.finance.dal.dataobject.receivable;

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
import java.time.LocalDateTime;

/**
 * 往来账核销明细 DO
 *
 * @author xyang
 */
@TableName("finance_receivable_payable_detail")
@KeySequence("finance_receivable_payable_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceReceivablePayableDetailDO extends TenantBaseDO {

    /**
     * 主键 ID
     */
    @TableId
    private Long id;

    private Long companyId;

    private Long arpId;

    private String writeOffNo;

    private Long transactionId;

    private Long voucherId;

    private BigDecimal amount;

    private LocalDateTime writeOffDate;

    private String description;

    private String remark;
}
