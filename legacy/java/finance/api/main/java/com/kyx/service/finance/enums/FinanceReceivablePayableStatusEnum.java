package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 往来账状态枚举
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinanceReceivablePayableStatusEnum implements ArrayValuable<String> {

    /**
     * 未结清
     */
    UNPAID("未结清"),
    /**
     * 部分结清
     */
    PARTIALLY_PAID("部分结清"),
    /**
     * 已结清
     */
    PAID("已结清"),
    /**
     * 已取消
     */
    CANCELLED("已取消");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinanceReceivablePayableStatusEnum::name)
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
