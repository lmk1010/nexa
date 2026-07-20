package com.kyx.service.finance.dal.dataobject.init;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kyx.foundation.tenant.core.db.TenantBaseDO;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 期初余额 DO
 * <p>
 * 余额记录规则：
 * - openingAmount 正数 = 该科目余额增加（收入增加 / 资产增加 / 支出增加）
 * - openingAmount 负数 = 该科目余额减少（冲销/退款等）
 * - 无需区分借贷方向，由 subjectType 决定业务含义
 *
 * @author xyang
 */
@TableName("finance_opening_balance")
@KeySequence("finance_opening_balance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FinanceOpeningBalanceDO extends TenantBaseDO {

    @TableId
    private Long id;

    /** 账套ID */
    private Long companyId;

    /** 期间，格式 yyyyMM */
    private String period;

    /** 科目编码 */
    private String subjectCode;

    /** 科目名称（冗余，方便展示） */
    private String subjectName;

    /**
     * 期初余额
     * 正数 = 余额增加，负数 = 余额减少（冲销）
     */
    private BigDecimal openingAmount;

    /** 备注 */
    private String remark;

    /**
     * 是否锁定：false-未锁定，true-已锁定
     * 锁定后不允许修改
     */
    private Boolean locked;

    /** 状态：0-启用，1-停用 */
    private Integer status;
}
