package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 结账状态枚举
 *
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinanceClosingStatusEnum implements ArrayValuable<String> {

    /**
     * 成功
     */
    SUCCESS("成功"),
    /**
     * 失败
     */
    FAILED("失败"),
    /**
     * 反结账
     */
    REVERSED("反结账");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinanceClosingStatusEnum::name)
            .toArray(String[]::new);

    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
