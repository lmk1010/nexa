package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 往来账类型枚举
 *
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinanceReceivablePayableTypeEnum implements ArrayValuable<String> {

    /**
     * 应收
     */
    RECEIVABLE("应收"),
    /**
     * 应付
     */
    PAYABLE("应付"),
    /**
     * 预收
     */
    ADVANCE_RECEIPT("预收"),
    /**
     * 预付
     */
    ADVANCE_PAYMENT("预付");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinanceReceivablePayableTypeEnum::name)
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
