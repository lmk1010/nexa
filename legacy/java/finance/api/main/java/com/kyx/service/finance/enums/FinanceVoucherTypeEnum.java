package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 凭证类型枚举
 *
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinanceVoucherTypeEnum implements ArrayValuable<String> {

    /**
     * 记账凭证
     */
    GENERAL("记账凭证"),
    /**
     * 收款凭证
     */
    RECEIPT("收款凭证"),
    /**
     * 付款凭证
     */
    PAYMENT("付款凭证"),
    /**
     * 转账凭证
     */
    TRANSFER("转账凭证");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinanceVoucherTypeEnum::name)
            .toArray(String[]::new);

    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
