package com.kyx.service.finance.enums;

import com.kyx.foundation.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 期间锁定状态枚举
 * @author xyang
 */
@Getter
@AllArgsConstructor
public enum FinancePeriodLockStatusEnum implements ArrayValuable<String> {

    /**
     * 已锁定
     */
    LOCKED("已锁定"),
    /**
     * 未锁定
     */
    UNLOCKED("未锁定");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(FinancePeriodLockStatusEnum::name)
            .toArray(String[]::new);

    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static FinancePeriodLockStatusEnum lockStatus(boolean state) {
        return state ? LOCKED : UNLOCKED;
    }
}
