package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 财务科目类型枚举
 * <p>
 * 对应业务逻辑：收入（经营收入/往来收入/资产收入）和支出（损益支出/往来支出）两大类。
 * <p>
 * 余额增减规则（替代 balance_dir）：
 * - INCOME 类科目：收入发生时金额增加（正数），退款/冲销时减少（负数）
 * - EXPENSE/COST 类科目：支出发生时金额增加（正数），冲销时减少（负数）
 * - ASSET 类科目：资产增加时金额增加（正数），减少时为负数
 * - LIABILITY/EQUITY 类科目：负债/权益增加时金额增加（正数），减少时为负数
 *
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinanceSubjectTypeEnum implements ArrayValuable<String> {

    /** 资产类 */
    ASSET("资产"),
    /** 负债类 */
    LIABILITY("负债"),
    /** 所有者权益类 */
    EQUITY("权益"),
    /** 成本类 */
    COST("成本"),
    /** 收入类（损益类） */
    INCOME("收入"),
    /** 费用类（损益类） */
    EXPENSE("费用");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinanceSubjectTypeEnum::name)
            .toArray(String[]::new);

    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    /**
     * 判断是否为损益类科目（收入/费用/成本），用于月末结转
     */
    public boolean isProfitLoss() {
        return this == INCOME || this == EXPENSE || this == COST;
    }

    /**
     * 判断是否为资产负债类科目，用于资产负债表
     */
    public boolean isBalanceSheet() {
        return this == ASSET || this == LIABILITY || this == EQUITY;
    }

    /**
     * 判断是否为收入类（金额增加代表收入增加）
     */
    public boolean isIncome() {
        return this == INCOME;
    }

    /**
     * 判断是否为支出/成本类（金额增加代表支出增加）
     */
    public boolean isExpenseOrCost() {
        return this == EXPENSE || this == COST;
    }
}
