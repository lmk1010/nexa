package com.kyx.service.finance.dal.dataobject.closing;

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

import java.time.LocalDateTime;

/**
 * 结账记录 DO
 *
 * @author xyang
 */
@TableName("finance_closing")
@KeySequence("finance_closing_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceClosingDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long companyId;

    private String closingPeriod;

    private String closingType;

    private String status;

    private String precheckResult;

    private Long profitTransferVoucherId;

    private LocalDateTime closeTime;

    private String closedBy;

    private LocalDateTime reversedTime;

    private String reversedBy;

    private String remark;
}
