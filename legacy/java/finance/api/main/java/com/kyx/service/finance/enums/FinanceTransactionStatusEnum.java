package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流水状态枚举
 *
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinanceTransactionStatusEnum implements ArrayValuable<String> {

    /**
     * 草稿
     */
    DRAFT("草稿"),
    /**
     * 成功
     */
    SUCCESS("成功"),
    /**
     * 作废
     */
    INVALID("作废");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinanceTransactionStatusEnum::name)
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
