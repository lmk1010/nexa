package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流水交易类型枚举
 *
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinanceTransactionTypeEnum implements ArrayValuable<String> {

    /**
     * 收入
     */
    INCOME("收入"),
    /**
     * 支出
     */
    EXPENSE("支出"),
    /**
     * 转账
     */
    TRANSFER("转账"),
    /**
     * 分摊
     */
    ALLOCATION("分摊");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinanceTransactionTypeEnum::name)
            .toArray(String[]::new);

    /**
     * 标签
     */
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
