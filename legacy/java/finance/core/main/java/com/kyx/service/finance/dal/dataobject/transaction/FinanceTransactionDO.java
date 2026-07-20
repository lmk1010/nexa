package com.kyx.service.finance.dal.dataobject.transaction;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import com.kyx.service.finance.enums.FinanceTransactionStatusEnum;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水 DO
 * @author xyang
 */
@TableName("finance_transaction")
@KeySequence("finance_transaction_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceTransactionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long companyId;

    private String transactionNo;

    private LocalDateTime transactionDate;

    private String transactionPeriod;

    private BigDecimal amount;

    private String transactionType;

    private Long accountId;

    private Long oppositeAccountId;

    private String subjectCode;

    private Long contactId;

    private String category;

    private String businessRefNo;

    private String description;

    /**
     * 状态 {@link FinanceTransactionStatusEnum}
     */
    private String status;

    private Long relatedBusinessId;

    private String businessType;

    private String tagJson;

    private String attachmentJson;

    private Long voucherId;

    private BigDecimal taxAmount;
}
