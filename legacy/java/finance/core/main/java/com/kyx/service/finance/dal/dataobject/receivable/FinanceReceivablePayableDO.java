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
 * 往来账 DO
 *
 * @author xyang
 */
@TableName("finance_receivable_payable")
@KeySequence("finance_receivable_payable_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceReceivablePayableDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long companyId;

    private Long contactId;

    private String billNo;

    private LocalDateTime billDate;

    private BigDecimal amount;

    private BigDecimal paidAmount;

    private BigDecimal balance;

    private String type;

    private String status;

    private LocalDateTime dueDate;

    private String subjectCode;

    private String sourceType;

    private String sourceNo;

    private String description;

    private String remark;
}
