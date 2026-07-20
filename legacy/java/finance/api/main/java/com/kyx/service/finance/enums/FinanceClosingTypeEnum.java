package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 结账类型枚举
 *
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinanceClosingTypeEnum implements ArrayValuable<String> {

    /**
     * 月结
     */
    MONTHLY("月结"),
    /**
     * 季结
     */
    QUARTERLY("季结"),
    /**
     * 年结
     */
    YEARLY("年结");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinanceClosingTypeEnum::name)
            .toArray(String[]::new);

    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
