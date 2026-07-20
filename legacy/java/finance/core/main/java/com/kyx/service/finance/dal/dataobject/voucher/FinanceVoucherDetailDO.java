package com.kyx.service.finance.dal.dataobject.voucher;

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
 * 凭证明细 DO
 *
 * @author xyang
 */
@TableName("finance_voucher_detail")
@KeySequence("finance_voucher_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceVoucherDetailDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long companyId;

    private Long voucherId;

    private Integer lineNo;

    private String subjectCode;

    private String subjectName;

    private Long contactId;

    private Long accountId;

    private BigDecimal debitAmount;

    private BigDecimal creditAmount;

    private BigDecimal taxAmount;

    private String description;
}
