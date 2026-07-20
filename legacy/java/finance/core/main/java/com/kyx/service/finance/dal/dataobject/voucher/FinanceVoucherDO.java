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
import java.time.LocalDateTime;

/**
 * 凭证 DO
 *
 * @author xyang
 */
@TableName("finance_voucher")
@KeySequence("finance_voucher_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceVoucherDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long companyId;

    private String voucherNo;

    private LocalDateTime voucherDate;

    private String voucherPeriod;

    private String voucherType;

    private String status;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private String sourceType;

    private String sourceNo;

    private Long sourceId;

    private String description;

    private String attachmentJson;

    private String approvedBy;

    private LocalDateTime approvedTime;

    private String postedBy;

    private LocalDateTime postedTime;
}
